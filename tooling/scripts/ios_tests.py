"""Execute XCTest on a disposable simulator; invoked by `test ios`."""

from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import shlex
import signal
import subprocess
import sys
import tempfile
import time
import uuid


class RunFailure(Exception):
    def __init__(self, message, status=1):
        super().__init__(message)
        self.status = status


def version(value):
    return tuple(int(part) for part in value.split('.'))


class IOSRun:
    def __init__(self, root, environment):
        self.root = root
        output = root / '.build/ios-tests'
        output.mkdir(parents=True, exist_ok=True)
        prefix = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ-')
        self.directory = Path(tempfile.mkdtemp(prefix=prefix, dir=output))
        self.log = (self.directory / 'run.log').open('w', buffering=1)
        self.name = 'NativeTemplate Tests ' + self.directory.name
        self.device = None
        self.create_attempted = False
        self.interrupted = 0
        self.cleaning = False
        self.summary = {'environment': environment, 'tests': {}, 'simulator_deleted': False}

    def note(self, message):
        print(message, flush=True)
        print(message, file=self.log, flush=True)

    def on_signal(self, signum, frame):
        # Do not raise between simctl creating a device and recording its UUID.
        self.interrupted = self.interrupted or 128 + signum

    def checkpoint(self):
        if self.interrupted and not self.cleaning:
            raise RunFailure('Test run interrupted.', self.interrupted)

    @staticmethod
    def stop(process):
        # Each native command owns a process group, never a user's Xcode session.
        try:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.communicate(timeout=5)
            except subprocess.TimeoutExpired:
                pass
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        process.communicate()

    def command(self, args, *, capture=False, timeout=60, log=None, interruptible=True):
        self.checkpoint()
        args = [str(arg) for arg in args]
        print('$ ' + shlex.join(args), file=self.log, flush=True)
        process = subprocess.Popen(args, cwd=self.root, start_new_session=True,
                                   stdout=subprocess.PIPE if capture else (log or self.log),
                                   stderr=log or self.log, text=True)
        deadline = time.monotonic() + timeout
        try:
            while True:
                if interruptible:
                    self.checkpoint()
                if time.monotonic() >= deadline:
                    raise RunFailure('Timed out: ' + shlex.join(args), 124)
                try:
                    stdout, _ = process.communicate(timeout=0.2)
                    break
                except subprocess.TimeoutExpired:
                    continue
        except BaseException:
            self.stop(process)
            raise
        if stdout:
            print(stdout, file=self.log, end='' if stdout.endswith('\n') else '\n', flush=True)
        if process.returncode:
            status = process.returncode if process.returncode > 0 else 128 - process.returncode
            raise RunFailure('Command failed: ' + shlex.join(args), status)
        return stdout or ''

    def select_simulator(self):
        config = (self.root / 'apps/ios/Config/Base.xcconfig').read_text()
        minimums = re.findall(r'^IPHONEOS_DEPLOYMENT_TARGET\s*=\s*([0-9.]+)\s*$', config, re.MULTILINE)
        if len(minimums) != 1:
            raise RunFailure('Expected one literal IPHONEOS_DEPLOYMENT_TARGET in Config/Base.xcconfig.')
        runtimes = json.loads(self.command(['xcrun', 'simctl', 'list', '-j', 'runtimes'], capture=True))
        candidates = [runtime for runtime in runtimes['runtimes']
                      if runtime.get('isAvailable')
                      and runtime['identifier'].startswith('com.apple.CoreSimulator.SimRuntime.iOS-')
                      and version(runtime['version']) >= version(minimums[0])]
        for runtime in sorted(candidates, key=lambda item: version(item['version']), reverse=True):
            phones = [device for device in runtime.get('supportedDeviceTypes', [])
                      if device.get('productFamily') == 'iPhone']
            if phones:
                return runtime, phones[0]
        raise RunFailure('No available iOS runtime with a compatible iPhone. Install an iOS '
                         'Simulator runtime in the selected Xcode (minimum ' + minimums[0] + ').')

    def execute(self, environment):
        self.command([self.root / 'tooling/scripts/doctor', 'ios'])
        runtime, device_type = self.select_simulator()
        self.summary.update(runtime=runtime['identifier'], device_type=device_type['identifier'])
        self.note('Simulator: ' + device_type['name'] + ' / ' + runtime['name'])
        self.create_attempted = True
        created = self.command(['xcrun', 'simctl', 'create', self.name,
                                device_type['identifier'], runtime['identifier']],
                               capture=True, interruptible=False).strip()
        self.device = str(uuid.UUID(created)).upper()
        self.summary['simulator'] = self.device
        self.command(['xcrun', 'simctl', 'bootstatus', self.device, '-b'], timeout=300)
        environments = ('dev', 'stg', 'prod') if environment == 'all' else (environment,)
        for selected in environments:
            title = selected.capitalize()
            bundle = self.directory / (selected + '.xcresult')
            log_path = self.directory / (selected + '.log')
            self.note('Running ' + title + ' app and component tests. Log: ' + str(log_path))
            with log_path.open('w') as test_log:
                self.command([
                    'xcodebuild', '-project', self.root / 'apps/ios/NativeTemplate.xcodeproj',
                    '-scheme', 'NativeTemplate-' + title, '-configuration', 'Debug-' + title,
                    '-destination', 'platform=iOS Simulator,id=' + self.device,
                    '-derivedDataPath', self.directory / 'derived-data' / selected,
                    '-resultBundlePath', bundle, '-parallel-testing-enabled', 'NO',
                    '-collect-test-diagnostics', 'never',
                    '-only-testing:AppTests', '-only-testing:DesignSystemUITests',
                    'CODE_SIGN_IDENTITY=-', 'test',
                ], timeout=1200, log=test_log)
            if not bundle.is_dir():
                raise RunFailure('Missing XCTest result bundle: ' + str(bundle))
            result = json.loads(self.command([
                'xcrun', 'xcresulttool', 'get', 'test-results', 'summary', '--path', bundle, '--compact',
            ], capture=True))
            self.summary['tests'][selected] = result
            if result.get('result') != 'Passed' or result.get('failedTests') != 0 or result.get('passedTests', 0) < 1:
                raise RunFailure('XCTest did not report executed, passing tests: ' + str(bundle))
            self.note(title + ': ' + str(result['passedTests']) + ' tests passed. Result: ' + str(bundle))

    def cleanup(self):
        self.cleaning = True
        try:
            # Recover only our unique device name if creation failed/timed out
            # after CoreSimulator created it but before its UUID was returned.
            if self.create_attempted and not self.device:
                listing = json.loads(self.command(['xcrun', 'simctl', 'list', '-j', 'devices'], capture=True))
                matches = [device['udid'] for devices in listing['devices'].values()
                           for device in devices if device.get('name') == self.name]
                if len(matches) > 1:
                    raise RunFailure('Multiple simulators match this run: ' + self.name)
                self.device = matches[0] if matches else None
            if self.device:
                try:
                    self.command(['xcrun', 'simctl', 'shutdown', self.device])
                except RunFailure as error:
                    self.note('Cleanup: shutdown failed; attempting deletion. ' + str(error))
                self.command(['xcrun', 'simctl', 'delete', self.device])
                self.summary['simulator_deleted'] = True
                self.note('Deleted test simulator: ' + self.device)
            return 0
        except (RunFailure, OSError, ValueError, KeyError) as error:
            self.note('Cleanup failed for ' + (self.device or self.name) + ': ' + str(error))
            return 1


def main(environment):
    run = IOSRun(Path(__file__).resolve().parents[2], environment)
    previous = {sig: signal.signal(sig, run.on_signal) for sig in (signal.SIGINT, signal.SIGTERM, signal.SIGHUP)}
    status = 0
    run.note('Test artifacts: ' + str(run.directory))
    try:
        run.execute(environment)
        run.checkpoint()
    except RunFailure as error:
        status = error.status
        run.note('Error: ' + str(error))
    except (OSError, ValueError, KeyError, TypeError) as error:
        status = 1
        run.note('Error: ' + str(error))
    finally:
        cleanup_status = run.cleanup()
        status = status or run.interrupted or cleanup_status
        run.summary['exit_code'] = status
        (run.directory / 'summary.json').write_text(json.dumps(run.summary, indent=2) + '\n')
        run.note('iOS tests: ' + ('passed' if status == 0 else 'failed (' + str(status) + ')'))
        run.note('Test artifacts: ' + str(run.directory))
        run.log.close()
        for sig, handler in previous.items():
            signal.signal(sig, handler)
    return status


if __name__ == '__main__':
    sys.exit(main(sys.argv[1]))

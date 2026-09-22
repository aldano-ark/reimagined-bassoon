"""Build and test this example, owning only a newly created simulator."""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import shlex
import signal
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]


class RunFailure(Exception):
    def __init__(self, message, status=1):
        super().__init__(message)
        self.status = status


class Commands:
    """Native commands have their own process group, reaped before cleanup."""
    def __init__(self, root, directory):
        self.root = root
        self.directory = directory
        self.interrupted = 0
        self.cleaning = False

    def on_signal(self, signum, _frame):
        # Do not throw between simulator creation and recording its identifier.
        self.interrupted = self.interrupted or 128 + signum

    def checkpoint(self):
        if self.interrupted and not self.cleaning:
            raise RunFailure('Verification interrupted.', self.interrupted)

    @staticmethod
    def stop(process):
        try:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.communicate(timeout=5)
            except subprocess.TimeoutExpired:
                pass
            # The direct process may have exited while its descendants remain.
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        process.communicate()

    def run(self, args, name, timeout=60, capture=False, interruptible=True):
        self.checkpoint()
        args = [str(a) for a in args]
        print(f'{name}…', flush=True)
        log_path = self.directory / (name + '.log')
        with log_path.open('w') as log:
            print('$ ' + shlex.join(args), file=log, flush=True)
            process = subprocess.Popen(args, cwd=self.root, start_new_session=True,
                                       stdout=subprocess.PIPE if capture else log, stderr=log, text=True)
            deadline = time.monotonic() + timeout
            try:
                while True:
                    if interruptible:
                        self.checkpoint()
                    if time.monotonic() >= deadline:
                        raise RunFailure('Timed out: ' + name, 124)
                    try:
                        output, _ = process.communicate(timeout=0.2)
                        break
                    except subprocess.TimeoutExpired:
                        continue
            except BaseException:
                self.stop(process)
                raise
            if output:
                print(output, file=log, flush=True)
        if process.returncode:
            print(log_path.read_text()[-12000:], file=sys.stderr)
            raise RunFailure('Command failed: ' + name, process.returncode if process.returncode > 0 else 128 - process.returncode)
        return output or ''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    scope = parser.add_mutually_exclusive_group()
    scope.add_argument('--unit-only', action='store_true', help='Run unit tests only (development feedback)')
    scope.add_argument('--ui-only', action='store_true', help='Run UI tests only (development feedback)')
    args = parser.parse_args()
    directory = ROOT / '.build' / ('verification-' + datetime.now().strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:6])
    directory.mkdir(parents=True)
    commands = Commands(ROOT, directory)
    print(f'Evidence: {directory.relative_to(ROOT)}', flush=True)
    name = 'PokemonCollection-' + directory.name
    device = None
    create_attempted = False
    failure = 0
    summary = {'simulator_deleted': False}
    handlers = {sig: signal.signal(sig, commands.on_signal) for sig in (signal.SIGINT, signal.SIGTERM, signal.SIGHUP)}
    try:
        runtimes = json.loads(commands.run(['xcrun', 'simctl', 'list', 'runtimes', '-j'], 'runtimes', capture=True))['runtimes']
        candidates = [r for r in runtimes if r.get('isAvailable') and r['identifier'].startswith('com.apple.CoreSimulator.SimRuntime.iOS-') and int(r['version'].split('.')[0]) >= 17 and any(d.get('productFamily') == 'iPhone' for d in r.get('supportedDeviceTypes', []))]
        if not candidates:
            raise RunFailure('No available iOS 17+ runtime with a compatible iPhone. Install one in Xcode.')
        runtime = max(candidates, key=lambda r: tuple(int(x) for x in r['version'].split('.')))
        kind = next(d for d in runtime['supportedDeviceTypes'] if d.get('productFamily') == 'iPhone')
        print(f"Dedicated simulator: {kind['name']} / {runtime['name']}", flush=True)
        create_attempted = True
        device = str(uuid.UUID(commands.run(['xcrun', 'simctl', 'create', name, kind['identifier'], runtime['identifier']], 'create', capture=True, interruptible=False).strip())).upper()
        summary.update(simulator=device, runtime=runtime['name'], device=kind['name'])
        commands.run(['xcrun', 'simctl', 'bootstatus', device, '-b'], 'bootstatus', 300)
        base = ['xcodebuild', '-project', 'PokemonCollection.xcodeproj', '-scheme', 'PokemonCollection', '-derivedDataPath', ROOT / '.build/DerivedData', '-parallel-testing-enabled', 'NO', 'CODE_SIGN_IDENTITY=-']
        destination = ['-destination', f'platform=iOS Simulator,id={device}']
        if not (args.unit_only or args.ui_only):
            commands.run(base + ['-configuration', 'Debug'] + destination + ['build-for-testing'], 'debug-build', 1200)
            commands.run(base + ['-configuration', 'Release', '-destination', 'generic/platform=iOS Simulator', 'build'], 'release-build', 1200)
        filters = ['-only-testing:CollectionTests'] if args.unit_only else (['-only-testing:CollectionUITests'] if args.ui_only else [])
        commands.run(base + ['-configuration', 'Debug'] + destination + filters + ['-collect-test-diagnostics', 'never', '-resultBundlePath', directory / 'Tests.xcresult', 'test'], 'tests', 1200)
        result = json.loads(commands.run(['xcrun', 'xcresulttool', 'get', 'test-results', 'summary', '--path', directory / 'Tests.xcresult'], 'results', capture=True))
        summary['tests'] = result
        if result.get('result') != 'Passed' or result.get('failedTests') != 0 or result.get('passedTests', 0) <= 0:
            raise RunFailure('Result bundle did not report executed, passing tests')
        print(f"Passed {result['passedTests']} tests.", flush=True)
        if not args.unit_only:
            commands.run(['xcrun', 'xcresulttool', 'export', 'attachments', '--path', directory / 'Tests.xcresult', '--output-path', directory / 'screenshots'], 'screenshots')
        commands.checkpoint()
    except RunFailure as error:
        print(str(error), file=sys.stderr)
        failure = error.status
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(str(error), file=sys.stderr)
        failure = 1
    finally:
        commands.cleaning = True
        try:
            if create_attempted and not device:
                listing = json.loads(commands.run(['xcrun', 'simctl', 'list', 'devices', '-j'], 'recover-device', capture=True))
                matches = [d['udid'] for devices in listing['devices'].values() for d in devices if d.get('name') == name]
                if len(matches) > 1:
                    raise RunFailure('Multiple simulators match this run: ' + name)
                device = matches[0] if matches else None
            if device:
                try:
                    commands.run(['xcrun', 'simctl', 'shutdown', device], 'shutdown')
                except RunFailure as error:
                    print(f'Shutdown failed; attempting deletion: {error}', file=sys.stderr)
                commands.run(['xcrun', 'simctl', 'delete', device], 'delete')
                summary['simulator_deleted'] = True
        except (RunFailure, OSError, ValueError, KeyError) as error:
            print(f'Cleanup failed for owned simulator {device or name}: {error}', file=sys.stderr)
            failure = failure or 1
        failure = failure or commands.interrupted
        summary['exit_code'] = failure
        (directory / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
        for sig, handler in handlers.items():
            signal.signal(sig, handler)
    return failure


if __name__ == '__main__':
    sys.exit(main())

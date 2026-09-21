"""Run the real test entry point with simulated Xcode/CoreSimulator processes."""

import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import sys
import tempfile
import time
import unittest


REPO = Path(__file__).resolve().parents[2]
FAKE_TOOL = r'''
import json
import os
from pathlib import Path
import signal
import sys
import time
import uuid

root = Path(os.environ['NATIVE_FAKE_ROOT'])
tool, args = Path(sys.argv[0]).name, sys.argv[1:]
with (root / 'trace.jsonl').open('a') as trace:
    trace.write(json.dumps({'tool': tool, 'args': args}) + '\n')
state_file = root / 'devices.json'
devices = json.loads(state_file.read_text())

if tool == 'uname':
    print('Darwin')
elif tool == 'plutil':
    raise AssertionError(args)
elif tool == 'xcodebuild':
    if args == ['-version']:
        print('Xcode 27.0\nBuild version 27A266a')
        sys.exit(0)
    assert args[-1] == 'test', args
    device = args[args.index('-destination') + 1].split('id=')[1]
    assert devices[device]['state'] == 'Booted' and device != 'personal-device'
    assert args[args.index('-parallel-testing-enabled') + 1] == 'NO'
    assert '-only-testing:AppTests' in args and '-only-testing:DesignSystemUITests' in args
    assert 'CODE_SIGN_IDENTITY=-' in args and 'CODE_SIGNING_ALLOWED=NO' not in args
    scheme = args[args.index('-scheme') + 1]
    assert args[args.index('-configuration') + 1] == 'Debug-' + scheme.split('-')[-1]
    result = Path(args[args.index('-resultBundlePath') + 1])
    derived = Path(args[args.index('-derivedDataPath') + 1])
    assert root / '.build/ios-tests' in result.parents
    assert result.parent in derived.parents and not result.exists()
    derived.mkdir(parents=True)
    if not os.environ.get('NATIVE_FAKE_MISSING_RESULT'):
        result.mkdir()
        (result / 'Info.plist').write_text('retained result')
    print('native test output', flush=True)
    if os.environ.get('NATIVE_FAKE_BLOCK_TEST'):
        def stop(signum, frame):
            (root / 'test-stopped').write_text(str(signum))
            sys.exit(128 + signum)
        signal.signal(signal.SIGTERM, stop)
        (root / 'test-started').write_text(str(os.getpid()))
        while True:
            time.sleep(0.05)
    sys.exit(int(os.environ.get('NATIVE_FAKE_TEST_STATUS', '0')))
elif tool == 'xcrun':
    if args == ['--sdk', 'iphonesimulator', '--show-sdk-path']:
        print(root / 'iOS SDK')
    elif args[:4] == ['xcresulttool', 'get', 'test-results', 'summary']:
        assert Path(args[args.index('--path') + 1]).is_dir()
        print(json.dumps({'result': 'Passed', 'passedTests': 0 if os.environ.get('NATIVE_FAKE_EMPTY_TESTS') else 15,
                          'failedTests': 0, 'skippedTests': 0, 'totalTestCount': 15}))
    else:
        assert args[0] == 'simctl', args
        action = args[1]
        status = int(os.environ.get('NATIVE_FAKE_' + action.upper() + '_STATUS', '0'))
        if status:
            sys.exit(status)
        if action == 'list' and 'runtimes' in args:
            print((root / 'runtimes.json').read_text())
        elif action == 'list' and 'devices' in args:
            print(json.dumps({'devices': {'runtime': [dict(v, udid=k) for k, v in devices.items()]}}))
        elif action == 'create':
            assert args[3:] == ['phone-27', 'com.apple.CoreSimulator.SimRuntime.iOS-27-0'], args
            device = str(uuid.uuid4()).upper()
            devices[device] = {'name': args[2], 'state': 'Shutdown'}
            state_file.write_text(json.dumps(devices))
            if os.environ.get('NATIVE_FAKE_CREATE_LOST_RESPONSE') == 'failure':
                sys.exit(73)
            if os.environ.get('NATIVE_FAKE_CREATE_LOST_RESPONSE') == 'invalid-uuid':
                print('invalid UUID response')
                sys.exit(0)
            print(device)
        elif action in ('bootstatus', 'shutdown', 'delete'):
            device = args[2]
            assert device != 'personal-device' and device in devices, args
            if action == 'bootstatus':
                assert args[3:] == ['-b']
                devices[device]['state'] = 'Booted'
            elif action == 'shutdown':
                assert not (root / 'test-started').exists() or (root / 'test-stopped').exists()
                devices[device]['state'] = 'Shutdown'
            else:
                del devices[device]
            state_file.write_text(json.dumps(devices))
        else:
            raise AssertionError(args)
else:
    raise AssertionError((tool, args))
'''


class IOSRunnerTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='native ios runner ')
        self.addCleanup(self.temporary.cleanup)
        self.outside = Path(self.temporary.name).resolve()
        self.root = self.outside / 'repo with spaces'
        shutil.copytree(REPO / 'tooling/scripts', self.root / 'tooling/scripts')
        (self.root / 'apps/ios/Config').mkdir(parents=True)
        shutil.copyfile(REPO / 'apps/ios/Config/Base.xcconfig', self.root / 'apps/ios/Config/Base.xcconfig')
        (self.root / 'fake tools').mkdir()
        (self.root / 'iOS SDK').mkdir()
        for tool in ('uname', 'xcodebuild', 'xcrun', 'plutil'):
            executable = self.root / 'fake tools' / tool
            executable.write_text('#!' + sys.executable + '\n' + FAKE_TOOL)
            executable.chmod(0o755)
        (self.root / 'fake tools/python3').symlink_to(sys.executable)
        self.personal = {'personal-device': {'name': 'Personal iPhone', 'state': 'Booted'}}
        (self.root / 'devices.json').write_text(json.dumps(self.personal))
        self.runtimes = [
            {'identifier': 'com.apple.CoreSimulator.SimRuntime.iOS-' + version.replace('.', '-'),
             'version': version, 'isAvailable': available, 'name': 'iOS ' + version,
             'supportedDeviceTypes': [
                 {'identifier': 'tablet', 'productFamily': 'iPad', 'name': 'iPad'},
                 {'identifier': 'phone-' + version.split('.')[0], 'productFamily': 'iPhone', 'name': 'iPhone'},
             ]}
            for version, available in [('16.4', True), ('27.0', True), ('28.0', False), ('17.0', True)]
        ]
        self.save_runtimes()
        self.environment = {k: v for k, v in os.environ.items()
                            if k != 'DEVELOPER_DIR' and not k.startswith('NATIVE_FAKE_')}
        self.environment.update(PATH=str(self.root / 'fake tools') + ':/usr/bin:/bin',
                                NATIVE_FAKE_ROOT=str(self.root),
                                JAVA_HOME='/missing/jdk', ANDROID_HOME='/missing/sdk')

    def save_runtimes(self):
        (self.root / 'runtimes.json').write_text(json.dumps({'runtimes': self.runtimes}))

    def run_cli(self, *args, **overrides):
        return subprocess.run(['/bin/sh', str(self.root / 'tooling/scripts/test'), *args],
                              cwd=self.outside, env={**self.environment, **overrides},
                              capture_output=True, text=True, timeout=15)

    def calls(self, tool):
        trace = self.root / 'trace.jsonl'
        calls = [json.loads(line) for line in trace.read_text().splitlines()] if trace.exists() else []
        return [call['args'] for call in calls if call['tool'] == tool]

    def assert_cleaned(self):
        self.assertEqual(json.loads((self.root / 'devices.json').read_text()), self.personal)

    def assert_status(self, result, expected):
        self.assertEqual(result.returncode, expected, result.stdout + result.stderr)

    def test_default_executes_both_targets_and_retains_results_from_outside_checkout(self):
        result = self.run_cli('ios')
        self.assert_status(result, 0)
        calls = [args for args in self.calls('xcodebuild') if args != ['-version']]
        self.assertEqual(len(calls), 1)
        self.assertIn('NativeTemplate-Dev', calls[0])
        bundles = list((self.root / '.build/ios-tests').glob('*/dev.xcresult/Info.plist'))
        self.assertEqual(len(bundles), 1)
        self.assertIn(str(bundles[0].parent.parent), result.stdout)
        self.assertIn('native test output', (bundles[0].parent.parent / 'dev.log').read_text())
        self.assert_cleaned()

    def test_all_environments_use_matching_schemes_and_distinct_results(self):
        self.assert_status(self.run_cli('ios', 'all'), 0)
        calls = [args for args in self.calls('xcodebuild') if args != ['-version']]
        self.assertEqual([args[args.index('-scheme') + 1] for args in calls],
                         ['NativeTemplate-Dev', 'NativeTemplate-Stg', 'NativeTemplate-Prod'])
        self.assertEqual(len({args[args.index('-resultBundlePath') + 1] for args in calls}), 3)
        self.assert_cleaned()

    def test_selected_environment_and_repeated_runs_preserve_earlier_evidence(self):
        for _ in range(2):
            self.assert_status(self.run_cli('ios', 'stg'), 0)
        self.assertEqual(len(list((self.root / '.build/ios-tests').glob('*/stg.xcresult'))), 2)
        self.assert_cleaned()

    def test_invalid_usage_has_no_native_side_effects(self):
        for args in [(), ('android',), ('all',), ('ios', 'bad'), ('ios', 'dev', 'extra')]:
            with self.subTest(args=args):
                result = self.run_cli(*args)
                self.assert_status(result, 2)
                self.assertIn('Usage:', result.stderr)
        self.assertFalse((self.root / 'trace.jsonl').exists())

    def test_no_compatible_runtime_fails_before_creating_device(self):
        for runtime in self.runtimes:
            runtime['isAvailable'] = runtime['version'] == '16.4'
        self.save_runtimes()
        result = self.run_cli('ios')
        self.assert_status(result, 1)
        self.assertIn('runtime', (result.stdout + result.stderr).lower())
        self.assertFalse(any(args[:2] == ['simctl', 'create'] for args in self.calls('xcrun')))
        self.assert_cleaned()

    def test_invalid_developer_directory_fails_before_simulator_creation(self):
        self.assert_status(self.run_cli('ios', DEVELOPER_DIR='/missing/xcode'), 1)
        self.assertFalse(any(args[:2] == ['simctl', 'create'] for args in self.calls('xcrun')))
        self.assert_cleaned()

    def test_native_failure_keeps_status_and_evidence_and_cleans_device(self):
        self.assert_status(self.run_cli('ios', 'all', NATIVE_FAKE_TEST_STATUS='65'), 65)
        self.assertEqual(len(list((self.root / '.build/ios-tests').glob('*/dev.xcresult'))), 1)
        self.assertEqual(len([args for args in self.calls('xcodebuild') if args != ['-version']]), 1)
        self.assert_cleaned()

    def test_boot_failure_cleans_device_and_retains_status(self):
        self.assert_status(self.run_cli('ios', NATIVE_FAKE_BOOTSTATUS_STATUS='71'), 71)
        self.assertEqual(self.calls('xcodebuild'), [['-version']])
        self.assert_cleaned()

    def test_partial_creation_recovers_only_the_run_owned_device(self):
        for response, status in [('failure', 73), ('invalid-uuid', 1)]:
            with self.subTest(response=response):
                self.assert_status(self.run_cli('ios', NATIVE_FAKE_CREATE_LOST_RESPONSE=response), status)
                self.assert_cleaned()
        self.assertEqual(self.calls('xcodebuild'), [['-version'], ['-version']])

    def test_cleanup_failure_is_visible_and_does_not_hide_native_failure(self):
        for native_status, expected in [('0', 1), ('65', 65)]:
            with self.subTest(native_status=native_status):
                result = self.run_cli('ios', NATIVE_FAKE_TEST_STATUS=native_status,
                                      NATIVE_FAKE_DELETE_STATUS='72')
                self.assert_status(result, expected)
                self.assertIn('cleanup', (result.stdout + result.stderr).lower())

    def test_missing_results_or_zero_executed_tests_cannot_pass(self):
        for flag in ('NATIVE_FAKE_MISSING_RESULT', 'NATIVE_FAKE_EMPTY_TESTS'):
            with self.subTest(flag=flag):
                self.assert_status(self.run_cli('ios', **{flag: '1'}), 1)
                self.assert_cleaned()

    def test_termination_stops_native_process_before_device_cleanup(self):
        process = subprocess.Popen(['/bin/sh', str(self.root / 'tooling/scripts/test'), 'ios'],
                                   cwd=self.outside,
                                   env={**self.environment, 'NATIVE_FAKE_BLOCK_TEST': '1'},
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        try:
            deadline = time.monotonic() + 10
            while not (self.root / 'test-started').exists() and process.poll() is None and time.monotonic() < deadline:
                time.sleep(0.02)
            self.assertTrue((self.root / 'test-started').exists(), 'native test process never started')
            process.send_signal(signal.SIGTERM)
            stdout, stderr = process.communicate(timeout=10)
            self.assertEqual(process.returncode, 143, stdout + stderr)
            self.assertTrue((self.root / 'test-stopped').exists())
            self.assert_cleaned()
        finally:
            if process.poll() is None:
                process.kill()
            process.communicate()


if __name__ == '__main__':
    unittest.main()

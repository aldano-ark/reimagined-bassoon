"""Exercise real shell dispatch against fake, slow external build tools.

The fake compilers enforce arguments and output placement. These tests validate
the wrapper contract; successful native builds are recorded separately.
"""

import concurrent.futures
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


REPO = Path(__file__).resolve().parents[2]
FAKE_TOOL = r'''
import json
import os
from pathlib import Path
import sys

root = Path(os.environ['NATIVE_FAKE_ROOT'])
tool = Path(sys.argv[0]).name
args = sys.argv[1:]
with (root / 'trace.jsonl').open('a') as trace:
    trace.write(json.dumps({'tool': tool, 'args': args, 'cwd': os.getcwd()}) + '\n')
if tool == 'uname':
    print(os.environ.get('NATIVE_FAKE_OS', 'Darwin'))
elif tool == 'java':
    print('openjdk version "' + os.environ.get('NATIVE_FAKE_JAVA_VERSION', '17.0.20') + '"', file=sys.stderr)
elif tool == 'xcrun':
    assert args == ['--sdk', 'iphonesimulator', '--show-sdk-path'], args
    print(root / 'iOS SDK')
elif tool == 'xcodebuild' and args == ['-version']:
    print('Xcode 27.0\nBuild version 27A266a')
elif tool == 'gradlew':
    assert Path.cwd() == root / 'apps/android', Path.cwd()
    assert args in (
        ['--no-daemon', ':app:assembleDebug'],
        ['--no-daemon', ':app:assembleDebug', ':app:lintDebug'],
    ), args
    print('android build')
    if os.environ.get('NATIVE_FAKE_NO_ARTIFACT') != '1':
        artifact = root / 'apps/android/app/build/outputs/apk/debug/app-debug.apk'
        artifact.parent.mkdir(parents=True, exist_ok=True)
        artifact.write_bytes(b'fake apk for wrapper tests')
    sys.exit(int(os.environ.get('NATIVE_FAKE_ANDROID_STATUS', '0')))
elif tool == 'xcodebuild':
    assert args == [
        '-project', str(root / 'apps/ios/NativeTemplate.xcodeproj'),
        '-scheme', 'NativeTemplate', '-configuration', 'Debug',
        '-destination', 'generic/platform=iOS Simulator',
        '-derivedDataPath', str(root / '.build/ios'),
        'CODE_SIGNING_ALLOWED=NO', 'build',
    ], args
    print('ios build')
    if os.environ.get('NATIVE_FAKE_NO_ARTIFACT') != '1':
        app = root / '.build/ios/Build/Products/Debug-iphonesimulator/NativeTemplate.app'
        app.mkdir(parents=True, exist_ok=True)
        executable = app / 'NativeTemplate'
        executable.write_text('fake executable for wrapper tests')
        executable.chmod(0o755)
        (app / 'Info.plist').write_text('fake plist for wrapper tests')
    sys.exit(int(os.environ.get('NATIVE_FAKE_IOS_STATUS', '0')))
else:
    raise AssertionError((tool, args))
'''


class CommandTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='native commands ')
        self.addCleanup(self.temporary.cleanup)
        self.outside = Path(self.temporary.name).resolve()
        self.root = self.outside / 'repo with spaces'
        scripts = self.root / 'tooling/scripts'
        if (REPO / 'tooling/scripts').is_dir():
            shutil.copytree(REPO / 'tooling/scripts', scripts)
        else:
            scripts.mkdir(parents=True)
        for directory in (
            'fake tools', 'Java Home/bin', 'Android SDK/platforms/android-36',
            'Android SDK/build-tools/36.0.0', 'iOS SDK',
            'apps/android/gradle/wrapper', 'apps/ios/NativeTemplate.xcodeproj',
        ):
            (self.root / directory).mkdir(parents=True, exist_ok=True)
        (self.root / 'Android SDK/platforms/android-36/android.jar').write_bytes(b'sdk')
        (self.root / 'apps/android/gradle/wrapper/gradle-wrapper.jar').write_bytes(b'wrapper')
        for name in ('uname', 'xcodebuild', 'xcrun'):
            self.fake_executable(self.root / 'fake tools' / name)
        self.fake_executable(self.root / 'Java Home/bin/java')
        self.fake_executable(self.root / 'apps/android/gradlew')
        self.environment = {
            key: value for key, value in os.environ.items()
            if key not in ('JAVA_HOME', 'ANDROID_HOME', 'ANDROID_SDK_ROOT', 'DEVELOPER_DIR')
            and not key.startswith('NATIVE_FAKE_')
        }
        self.environment.update({
            'PATH': str(self.root / 'fake tools') + ':/usr/bin:/bin',
            'JAVA_HOME': str(self.root / 'Java Home'),
            'ANDROID_HOME': str(self.root / 'Android SDK'),
            'NATIVE_FAKE_ROOT': str(self.root),
        })

    def fake_executable(self, path):
        path.write_text('#!' + sys.executable + '\n' + FAKE_TOOL)
        path.chmod(0o755)

    def run_cli(self, action, *arguments, **overrides):
        return subprocess.run(
            ['/bin/sh', str(self.root / 'tooling/scripts' / action), *arguments],
            cwd=self.outside, env={**self.environment, **overrides},
            text=True, capture_output=True, timeout=30,
        )

    def calls(self):
        trace = self.root / 'trace.jsonl'
        return [json.loads(line) for line in trace.read_text().splitlines()] if trace.exists() else []

    def artifact(self, platform):
        if platform == 'android':
            return self.root / 'apps/android/app/build/outputs/apk/debug/app-debug.apk'
        return self.root / '.build/ios/Build/Products/Debug-iphonesimulator/NativeTemplate.app/NativeTemplate'

    def assert_success(self, result):
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_paths_with_spaces_from_another_directory(self):
        for platform in ('android', 'ios'):
            with self.subTest(platform=platform):
                self.assert_success(self.run_cli('build', platform))
                self.assertTrue(self.artifact(platform).is_file())

    def test_invalid_usage_dispatches_nothing(self):
        for action in ('doctor', 'build', 'verify'):
            for args in ((), ('harmonyos',), ('android', 'extra'), ('--help',)):
                with self.subTest(action=action, args=args):
                    result = self.run_cli(action, *args)
                    self.assertEqual(result.returncode, 2)
                    self.assertIn('Usage:', result.stderr)
        self.assertEqual(self.calls(), [])

    def test_invalid_java_does_not_block_ios(self):
        result = self.run_cli('doctor', 'android', JAVA_HOME='/nonexistent/jdk')
        self.assertEqual(result.returncode, 1)
        self.assertIn('JAVA_HOME', result.stdout + result.stderr)
        self.assert_success(self.run_cli('build', 'ios', JAVA_HOME='/nonexistent/jdk'))
        self.assertNotIn('java', [call['tool'] for call in self.calls()])

    def test_invalid_xcode_does_not_block_android(self):
        result = self.run_cli('doctor', 'ios', DEVELOPER_DIR='/nonexistent/xcode')
        self.assertEqual(result.returncode, 1)
        self.assertIn('DEVELOPER_DIR', result.stdout + result.stderr)
        self.assert_success(self.run_cli('build', 'android', DEVELOPER_DIR='/nonexistent/xcode'))
        self.assertNotIn('xcodebuild', [call['tool'] for call in self.calls()])

    def test_invalid_android_sdk_does_not_block_ios(self):
        result = self.run_cli('doctor', 'android', ANDROID_HOME='/nonexistent/sdk')
        self.assertEqual(result.returncode, 1)
        self.assertIn('ANDROID_HOME', result.stdout + result.stderr)
        self.assert_success(self.run_cli('build', 'ios', ANDROID_HOME='/nonexistent/sdk'))

    def test_wrong_java_version_fails_before_gradle(self):
        result = self.run_cli('build', 'android', NATIVE_FAKE_JAVA_VERSION='16.0.2')
        self.assertEqual(result.returncode, 1)
        self.assertIn('17', result.stdout + result.stderr)
        self.assertNotIn('gradlew', [call['tool'] for call in self.calls()])

    def test_bundled_android_studio_java_is_supported(self):
        self.assert_success(self.run_cli('verify', 'android', NATIVE_FAKE_JAVA_VERSION='25.0.3'))

    def test_unsupported_future_java_fails_before_gradle(self):
        result = self.run_cli('build', 'android', NATIVE_FAKE_JAVA_VERSION='27.0.1')
        self.assertEqual(result.returncode, 1)
        self.assertNotIn('gradlew', [call['tool'] for call in self.calls()])

    def test_native_failure_is_not_hidden_by_stale_artifact(self):
        self.assert_success(self.run_cli('build', 'android'))
        result = self.run_cli('verify', 'android', NATIVE_FAKE_ANDROID_STATUS='73')
        self.assertEqual(result.returncode, 73, result.stdout + result.stderr)
        self.assertTrue(self.artifact('android').exists())
        log = self.root / '.build/logs/android/verify.log'
        self.assertIn('android build', log.read_text())

    def test_ios_native_failure_retains_status(self):
        result = self.run_cli('verify', 'ios', NATIVE_FAKE_IOS_STATUS='65')
        self.assertEqual(result.returncode, 65, result.stdout + result.stderr)

    def test_all_attempts_both_and_preserves_first_failure(self):
        result = self.run_cli('verify', 'all', NATIVE_FAKE_ANDROID_STATUS='73', NATIVE_FAKE_IOS_STATUS='65')
        self.assertEqual(result.returncode, 73, result.stdout + result.stderr)
        self.assertTrue(self.artifact('ios').exists())
        self.assertIn('android: failed', result.stderr)
        self.assertIn('ios: failed', result.stderr)

    def test_all_attempts_available_platform_after_prerequisite_failure(self):
        result = self.run_cli('build', 'all', JAVA_HOME='/nonexistent/jdk')
        self.assertEqual(result.returncode, 1)
        self.assertTrue(self.artifact('ios').exists())
        self.assertFalse(self.artifact('android').exists())

    def test_success_without_artifact_fails_verification(self):
        for platform in ('android', 'ios'):
            with self.subTest(platform=platform):
                result = self.run_cli('verify', platform, NATIVE_FAKE_NO_ARTIFACT='1')
                self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
                self.assertIn('artifact', result.stdout + result.stderr)

    def test_verify_runs_android_lint(self):
        self.assert_success(self.run_cli('verify', 'android'))
        calls = [call for call in self.calls() if call['tool'] == 'gradlew']
        self.assertEqual(len(calls), 1)
        self.assertIn(':app:lintDebug', calls[0]['args'])

    def test_doctor_creates_no_artifact(self):
        self.assert_success(self.run_cli('doctor', 'all'))
        for platform in ('android', 'ios'):
            self.assertFalse(self.artifact(platform).exists())
            self.assertTrue((self.root / '.build/logs' / platform / 'doctor.log').is_file())

    def test_concurrent_platforms_have_separate_logs_and_outputs(self):
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            results = list(executor.map(lambda platform: self.run_cli('build', platform), ('android', 'ios')))
        for result in results:
            self.assert_success(result)
        for platform in ('android', 'ios'):
            self.assertTrue(self.artifact(platform).is_file())
            log = self.root / '.build/logs' / platform / 'build.log'
            self.assertIn(platform + ' build', log.read_text())


if __name__ == '__main__':
    unittest.main()

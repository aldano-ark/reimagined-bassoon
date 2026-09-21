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
import plistlib
import sys

root = Path(os.environ['NATIVE_FAKE_ROOT'])
tool = Path(sys.argv[0]).name
args = sys.argv[1:]
with (root / 'trace.jsonl').open('a') as trace:
    trace.write(json.dumps({'tool': tool, 'args': args, 'cwd': os.getcwd()}) + '\n')

def leaf(path):
    result = {}
    for raw in path.read_text().splitlines():
        line = raw.strip()
        if line and not line.startswith(('#', '//')):
            key, value = line.split('=', 1)
            result[key.strip()] = value.strip().replace('$()', '') if path.suffix == '.xcconfig' else value.strip()
    return result

def missing(variant):
    return os.environ.get('NATIVE_FAKE_NO_ARTIFACT') == '1' or os.environ.get('NATIVE_FAKE_MISSING_VARIANT') == variant

def corrupted(key, variant, value):
    return 'wrong-value' if os.environ.get('NATIVE_FAKE_BAD_' + key + '_VARIANT') == variant else value

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
    verify = ['--no-daemon']
    variants = []
    for env in ('Dev', 'Stg', 'Prod'):
        for mode in ('Debug', 'Release'):
            variant = env + mode
            verify += [f':app:assemble{variant}', f':app:lint{variant}', f':app:test{variant}UnitTest']
            variants.append((env.lower(), mode.lower(), env.lower() + mode))
    verify += [':core:designsystem:lintDebug', ':core:designsystem:testDebugUnitTest']
    assert args in (['--no-daemon', ':app:assembleDevDebug'], verify), args
    if args != verify:
        variants = [('dev', 'debug', 'devDebug')]
    print('android build')
    for env, mode, variant in variants:
        if not missing(variant):
            values = leaf(root / f'apps/android/config/{env}.properties')
            suffix = '-unsigned' if mode == 'release' else ''
            artifact = root / f'apps/android/app/build/outputs/apk/{env}/{mode}/app-{env}-{mode}{suffix}.apk'
            artifact.parent.mkdir(parents=True, exist_ok=True)
            artifact.write_text(json.dumps({
                'id': corrupted('ID', variant, values['APP_ID']),
                'name': corrupted('NAME', variant, values['APP_DISPLAY_NAME']),
            }))
    status = int(os.environ.get('NATIVE_FAKE_ANDROID_STATUS', '0'))
    if os.environ.get('NATIVE_FAKE_FAIL_VARIANT') in [v for _, _, v in variants]:
        status = status or 76
    if status == 0 and args == verify:
        status = int(os.environ.get('NATIVE_FAKE_APP_TEST_STATUS', '0')) or int(os.environ.get('NATIVE_FAKE_LIBRARY_TEST_STATUS', '0'))
    sys.exit(status)
elif tool == 'aapt2':
    assert args[:2] == ['dump', 'badging'], args
    status = int(os.environ.get('NATIVE_FAKE_BADGING_STATUS', '0'))
    if status:
        sys.exit(status)
    values = json.loads(Path(args[2]).read_text())
    print("package: name='" + values['id'] + "' versionCode='1' versionName='0.1.0'")
    print("application-label:'" + values['name'] + "'")
elif tool == 'xcodebuild':
    assert args[-1] in ('build', 'build-for-testing'), args
    scheme = args[args.index('-scheme') + 1]
    configuration = args[args.index('-configuration') + 1]
    mode, env = configuration.split('-')
    assert mode in ('Debug', 'Release') and env in ('Dev', 'Stg', 'Prod'), args
    assert scheme == 'NativeTemplate-' + env, args
    assert args == [
        '-project', str(root / 'apps/ios/NativeTemplate.xcodeproj'),
        '-scheme', scheme, '-configuration', configuration,
        '-destination', 'generic/platform=iOS Simulator',
        '-derivedDataPath', str(root / '.build/ios'),
        'CODE_SIGNING_ALLOWED=NO', args[-1],
    ], args
    assert mode == 'Debug' or args[-1] == 'build'
    print('ios build')
    if not missing(configuration):
        values = leaf(root / f'apps/ios/Config/{env}.xcconfig')
        app = root / f'.build/ios/Build/Products/{configuration}-iphonesimulator/NativeTemplate.app'
        app.mkdir(parents=True, exist_ok=True)
        executable = app / 'NativeTemplate'
        executable.write_text('fake executable for wrapper tests')
        executable.chmod(0o755)
        info = {
            'CFBundleIdentifier': corrupted('ID', configuration, values['APP_ID']),
            'CFBundleDisplayName': corrupted('NAME', configuration, values['APP_DISPLAY_NAME']),
            'AppEnvironment': corrupted('ENV', configuration, values['APP_ENVIRONMENT']),
            'APIBaseURL': corrupted('URL', configuration, values['API_BASE_URL']),
        }
        info.pop(os.environ.get('NATIVE_FAKE_MISSING_PLIST_KEY'), None)
        (app / 'Info.plist').write_bytes(plistlib.dumps(info))
    status = int(os.environ.get('NATIVE_FAKE_IOS_STATUS', '0'))
    if os.environ.get('NATIVE_FAKE_FAIL_VARIANT') == configuration:
        status = status or 76
    if status == 0 and args[-1] == 'build-for-testing':
        status = int(os.environ.get('NATIVE_FAKE_IOS_TEST_BUILD_STATUS', '0'))
    sys.exit(status)
elif tool == 'plutil':
    assert len(args) == 6 and args[0] == '-extract' and args[2:5] == ['raw', '-o', '-'], args
    info = plistlib.loads(Path(args[-1]).read_bytes())
    if args[1] not in info:
        sys.exit(1)
    print(info[args[1]])
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
        for name in ('uname', 'xcodebuild', 'xcrun', 'plutil'):
            self.fake_executable(self.root / 'fake tools' / name)
        self.fake_executable(self.root / 'Java Home/bin/java')
        self.fake_executable(self.root / 'apps/android/gradlew')
        self.fake_executable(self.root / 'Android SDK/build-tools/36.0.0/aapt2')
        for platform, folder, names in (
            ('android', 'config', ['dev.properties', 'stg.properties', 'prod.properties']),
            ('ios', 'Config', ['Dev.xcconfig', 'Stg.xcconfig', 'Prod.xcconfig']),
        ):
            target = self.root / 'apps' / platform / folder
            target.mkdir(parents=True, exist_ok=True)
            for name in names:
                shutil.copyfile(REPO / 'apps' / platform / folder / name, target / name)
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
            return self.root / 'apps/android/app/build/outputs/apk/dev/debug/app-dev-debug.apk'
        return self.root / '.build/ios/Build/Products/Debug-Dev-iphonesimulator/NativeTemplate.app/NativeTemplate'

    def assert_success(self, result):
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_paths_with_spaces_from_another_directory(self):
        for platform in ('android', 'ios'):
            with self.subTest(platform=platform):
                self.assert_success(self.run_cli('build', platform))
                self.assertTrue(self.artifact(platform).is_file())

    def test_invalid_usage_dispatches_nothing(self):
        for action in ('doctor', 'build', 'verify'):
            for args in ((), ('unsupported',), ('android', 'extra'), ('--help',)):
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
        self.assertIn(':app:lintDevDebug', calls[0]['args'])

    def test_verify_includes_design_library_checks(self):
        self.assert_success(self.run_cli('verify', 'android'))
        calls = [call for call in self.calls() if call['tool'] == 'gradlew']
        self.assertEqual(len(calls), 1)
        self.assertIn(':core:designsystem:lintDebug', calls[0]['args'])
        self.assertIn(':core:designsystem:testDebugUnitTest', calls[0]['args'])

    def test_library_test_failure_is_not_hidden_by_app_artifact(self):
        result = self.run_cli('verify', 'android', NATIVE_FAKE_LIBRARY_TEST_STATUS='74')
        self.assertEqual(result.returncode, 74)
        self.assertTrue(self.artifact('android').exists())

    def test_ios_verify_compiles_test_target(self):
        self.assert_success(self.run_cli('verify', 'ios'))
        calls = [call for call in self.calls()
                 if call['tool'] == 'xcodebuild' and call['args'] != ['-version']]
        self.assertEqual(calls[0]['args'][-1], 'build-for-testing')

    def test_ios_test_compilation_failure_is_propagated(self):
        result = self.run_cli('verify', 'ios', NATIVE_FAKE_IOS_TEST_BUILD_STATUS='75')
        self.assertEqual(result.returncode, 75)
        self.assertTrue(self.artifact('ios').exists())

    def test_verify_dispatches_all_android_variants(self):
        self.assert_success(self.run_cli('verify', 'android'))
        calls = [call for call in self.calls() if call['tool'] == 'gradlew']
        expected = ['--no-daemon']
        for environment in ('Dev', 'Stg', 'Prod'):
            for mode in ('Debug', 'Release'):
                variant = environment + mode
                expected += [f':app:assemble{variant}', f':app:lint{variant}',
                             f':app:test{variant}UnitTest']
        expected += [':core:designsystem:lintDebug', ':core:designsystem:testDebugUnitTest']
        self.assertEqual(len(calls), 1)
        self.assertEqual(calls[0]['args'], expected)

    def test_verify_dispatches_all_ios_variants(self):
        self.assert_success(self.run_cli('verify', 'ios'))
        calls = [call for call in self.calls()
                 if call['tool'] == 'xcodebuild' and call['args'] != ['-version']]
        actual = [(call['args'][call['args'].index('-scheme') + 1],
                   call['args'][call['args'].index('-configuration') + 1],
                   call['args'][-1]) for call in calls]
        expected = [(f'NativeTemplate-{env}', f'{mode}-{env}',
                     'build-for-testing' if mode == 'Debug' else 'build')
                    for env in ('Dev', 'Stg', 'Prod') for mode in ('Debug', 'Release')]
        self.assertEqual(actual, expected)

    def test_missing_staging_release_artifact_does_not_pass(self):
        for platform, variant in [('android', 'stgRelease'), ('ios', 'Release-Stg')]:
            with self.subTest(platform=platform):
                self.assert_success(self.run_cli('build', platform))
                result = self.run_cli('verify', platform, NATIVE_FAKE_MISSING_VARIANT=variant)
                self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
                self.assertIn('artifact', result.stdout + result.stderr)

    def test_wrong_identity_or_name_does_not_pass(self):
        for platform, variant in [('android', 'stgRelease'), ('ios', 'Release-Stg')]:
            for field in ('ID', 'NAME'):
                with self.subTest(platform=platform, field=field):
                    result = self.run_cli('verify', platform,
                                          **{f'NATIVE_FAKE_BAD_{field}_VARIANT': variant})
                    self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
                    self.assertIn('mismatch', result.stdout + result.stderr)

    def test_wrong_ios_endpoint_or_environment_does_not_pass(self):
        for field, key in [('URL', 'APIBaseURL'), ('ENV', 'AppEnvironment')]:
            with self.subTest(field=field):
                result = self.run_cli('verify', 'ios',
                                      **{f'NATIVE_FAKE_BAD_{field}_VARIANT': 'Release-Stg'})
                self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
                self.assertIn(key, result.stdout + result.stderr)

    def test_missing_ios_plist_value_fails(self):
        result = self.run_cli('verify', 'ios', NATIVE_FAKE_MISSING_PLIST_KEY='APIBaseURL')
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn('APIBaseURL', result.stdout + result.stderr)

    def test_later_variant_failure_retains_native_status(self):
        for platform, variant in [('android', 'stgRelease'), ('ios', 'Release-Stg')]:
            with self.subTest(platform=platform):
                self.assert_success(self.run_cli('build', platform))
                result = self.run_cli('verify', platform, NATIVE_FAKE_FAIL_VARIANT=variant)
                self.assertEqual(result.returncode, 76, result.stdout + result.stderr)
                self.assertTrue(self.artifact(platform).is_file())

    def test_app_test_failure_retains_status(self):
        result = self.run_cli('verify', 'android', NATIVE_FAKE_APP_TEST_STATUS='77')
        self.assertEqual(result.returncode, 77, result.stdout + result.stderr)

    def test_badging_failure_is_an_artifact_error(self):
        result = self.run_cli('verify', 'android', NATIVE_FAKE_BADGING_STATUS='78')
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn('artifact', result.stdout + result.stderr)

    def test_missing_android_metadata_tool_does_not_block_ios(self):
        (self.root / 'Android SDK/build-tools/36.0.0/aapt2').unlink()
        result = self.run_cli('doctor', 'android')
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn('aapt2', result.stdout + result.stderr)
        self.assert_success(self.run_cli('build', 'ios'))

    def test_settings_reader_rejects_missing_blank_and_duplicate_keys(self):
        path = self.root / 'settings with spaces.properties'
        for content in ('APP_ENVIRONMENT=dev\n', 'APP_ID=\n', 'APP_ID=one\nAPP_ID=two\n'):
            path.write_text(content)
            result = subprocess.run(
                ['/bin/sh', '-c', '. "$1"; native_setting "$2" APP_ID', 'settings-test',
                 str(self.root / 'tooling/scripts/lib.sh'), str(path)],
                text=True, capture_output=True, env=self.environment,
            )
            self.assertEqual(result.returncode, 1, result.stdout + result.stderr)

    def test_settings_reader_preserves_platform_specific_url_literals(self):
        for extension, raw, expected in (
            ('properties', 'https://api.example.com/$()/v1', 'https://api.example.com/$()/v1'),
            ('xcconfig', 'https:/$()/api.example.com/v1', 'https://api.example.com/v1'),
        ):
            path = self.root / ('settings with spaces.' + extension)
            path.write_text('API_BASE_URL = ' + raw + '\n')
            result = subprocess.run(
                ['/bin/sh', '-c', '. "$1"; native_setting "$2" API_BASE_URL', 'settings-test',
                 str(self.root / 'tooling/scripts/lib.sh'), str(path)],
                text=True, capture_output=True, env=self.environment,
            )
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertEqual(result.stdout.strip(), expected)

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

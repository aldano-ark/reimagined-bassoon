"""Native configuration parity and shared Xcode scheme contracts."""
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

REPO = Path(__file__).resolve().parents[2]


def read_leaf(path):
    required = {'APP_ENVIRONMENT', 'APP_ID', 'APP_DISPLAY_NAME', 'API_BASE_URL'}
    values = {}
    for raw in path.read_text(encoding='utf-8').splitlines():
        line = raw.strip()
        if not line or line.startswith(('#', '//')):
            continue
        key, separator, value = line.partition('=')
        key, value = key.strip(), value.strip()
        if not separator or key not in required or key in values or not value:
            raise ValueError(f'{path}: invalid or duplicate key {key}')
        values[key] = value.replace('$()', '') if path.suffix == '.xcconfig' else value
    if set(values) != required:
        raise ValueError(f'{path}: missing keys {sorted(required - set(values))}')
    return values


class EnvironmentConfigurationTests(unittest.TestCase):
    def test_native_leaf_configuration_agrees(self):
        identities = set()
        for environment, title in [('dev', 'Dev'), ('stg', 'Stg'), ('prod', 'Prod')]:
            with self.subTest(environment=environment):
                android = read_leaf(REPO / f'apps/android/config/{environment}.properties')
                ios = read_leaf(REPO / f'apps/ios/Config/{title}.xcconfig')
                self.assertEqual(android, ios)
                self.assertEqual(android['APP_ENVIRONMENT'], environment)
                suffix = '' if environment == 'prod' else '.' + environment
                self.assertEqual(android['APP_ID'], 'com.example.nativetemplate' + suffix)
                identities.add(android['APP_ID'])
        self.assertEqual(len(identities), 3)

    def test_scheme_actions_select_matching_environment_and_mode(self):
        for environment in ('Dev', 'Stg', 'Prod'):
            with self.subTest(environment=environment):
                path = REPO / f'apps/ios/NativeTemplate.xcodeproj/xcshareddata/xcschemes/NativeTemplate-{environment}.xcscheme'
                scheme = ET.parse(path).getroot()
                for action in ('LaunchAction', 'TestAction', 'AnalyzeAction'):
                    self.assertEqual(scheme.find(action).get('buildConfiguration'), f'Debug-{environment}')
                for action in ('ProfileAction', 'ArchiveAction'):
                    self.assertEqual(scheme.find(action).get('buildConfiguration'), f'Release-{environment}')
                testables = scheme.findall('TestAction/Testables/TestableReference')
                self.assertEqual({test.find('BuildableReference').get('BlueprintName') for test in testables},
                                 {'AppTests', 'DesignSystemUITests'})
                self.assertTrue(all(test.get('skipped') == 'NO' for test in testables))
                self.assertEqual(scheme.find('TestAction/MacroExpansion/BuildableReference').get('BlueprintName'),
                                 'NativeTemplate')
                for entry in scheme.findall('BuildAction/BuildActionEntries/BuildActionEntry'):
                    if entry.find('BuildableReference').get('BlueprintName') in ('AppTests', 'DesignSystemUITests'):
                        self.assertEqual(entry.get('buildForTesting'), 'YES')
                        self.assertEqual(entry.get('buildForArchiving'), 'NO')
                        self.assertEqual(entry.get('buildForRunning'), 'NO')

    def test_malformed_leaf_cannot_silently_pass_parity(self):
        valid = (REPO / 'apps/android/config/dev.properties').read_text()
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'config.properties'
            for content in (valid + 'APP_ID=other\n', valid.replace('APP_ENVIRONMENT=dev\n', ''),
                            valid.replace('APP_ENVIRONMENT=dev', 'APP_ENVIRONMENT='),
                            valid + 'UNKNOWN=value\n'):
                with self.subTest(content=content):
                    path.write_text(content)
                    with self.assertRaises(ValueError):
                        read_leaf(path)


if __name__ == '__main__':
    unittest.main()

# Native Mobile Template Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create buildable, empty Android and iOS applications in a native monorepo with discoverable architecture, contributor guidance, and verified build isolation.

**Architecture:** Keep independent Gradle and Xcode projects under `apps/`, with documented feature/core extension boundaries inside each platform. Shared POSIX shell commands invoke native tools without coupling their prerequisites or outputs. Lightweight documentation and task templates support humans and agents using the same workflow.

**Tech Stack:** Kotlin, Jetpack Compose, Gradle Kotlin DSL, Swift 6, SwiftUI, Xcode, POSIX shell; Python's standard-library `unittest` for command-wrapper tests only.

**Spec:** [Approved design](../specs/2026-09-20-native-mobile-template-design.md). Read it before execution.

**Status:** Approved for native execution. Both application shells and the shared commands are implemented and verified; final integration evidence and review remain in progress.

**Execution update:** Per the user's preference, accept Gradle-compatible launcher JDKs 17–26 instead of restricting the launcher to JDK 17. JBR 25.0.3 bundled with Android Studio passed Android build/lint. The IDE-generated project daemon criteria select Java 25; the app's bytecode target remains 17. Historical JDK-17-only instructions below are superseded by this update and the current platform README.

## Global Constraints

- Use Jetpack Compose for Android and SwiftUI for iOS.
- Support Android API 26 and later, and iOS 17 and later, as initial deployment minimums. These are independent of the installed compile SDK versions.
- Use `NativeTemplate` as the temporary application name and `com.example.nativetemplate` as the application/bundle identifier. Document all rename locations.
- Keep a normal Gradle project and a committed Xcode project with a shared scheme. Opening either platform in its native IDE requires no project-generation step.
- Use POSIX shell entry points for shared commands. Native builds do not depend on Node.js, Ruby, a monorepo framework, or the other platform's SDK.
- The initial Android build has one application module; the initial iOS project has one application target.
- Project caches, build outputs, logs, IDE user settings, `local.properties`, and signing material are ignored by version control.
- Product features, network/storage implementations, shared design assets, release environments, signing automation, hosted CI, and agent-runner integrations are separate additions to this foundation.
- HarmonyOS is documentation-only and excluded from `all` commands in this version.
- No trivial app tests: native compilation and Android lint verify the empty shells; command tests verify consequential dispatch and failure behavior.
- Native builds must not modify the other platform's project, tracked files, or global SDK selections.

## Review Focus

1. A repository or toolchain path containing spaces, invoked from another directory, must preserve paths and arguments. Test in Task 3 and exercise real commands in Task 4.
2. An explicitly invalid toolchain path must fail for its platform without breaking the other platform's command or silently selecting a different toolchain. Test in Task 3.
3. A failing native process with a stale artifact present must still fail and retain the native exit code. Test in Task 3.
4. An aggregate command with one failing platform must report both results and fail overall; concurrent platform builds must keep their outputs separate. Test in Task 3 and verify real concurrency in Task 4.
5. A zero exit status with no expected application artifact must fail verification rather than produce a false success report. Test in Task 3.

## File ownership and interfaces

| Unit | Files | Responsibility |
| --- | --- | --- |
| Android | `apps/android/` | Kotlin source, resources, pinned Gradle build and wrapper, Android extension guidance |
| iOS | `apps/ios/` | Swift source, resources, committed Xcode project and shared scheme, iOS extension guidance |
| Commands | `tooling/scripts/{doctor,build,verify,lib.sh}` | Argument validation, prerequisite checks, native invocation, logs, artifacts, status aggregation |
| Command tests | `tooling/tests/test_commands.py` | Isolated fake-tool tests; no SDK installation or real app compilation |
| Shared conventions | Root documents, `docs/`, `contracts/`, `tooling/templates/` | Architecture boundaries, workflows, reusable work definitions, evidence |

Public commands are exactly `doctor android|ios|all`, `build android|ios|all`, and `verify android|ios|all`, called through `./tooling/scripts/`. They take one platform argument and no other flags. Exit 2 means invalid command usage, exit 1 means a prerequisite or artifact failure, and a native failure retains its original nonzero status. An aggregate command returns the first platform failure after attempting both platforms.

Platform artifact contracts:

- Android: `apps/android/app/build/outputs/apk/debug/app-debug.apk`.
- iOS: `.build/ios/Build/Products/Debug-iphonesimulator/NativeTemplate.app`, including its executable and `Info.plist`.
- Logs: `.build/logs/android/{doctor,build,verify}.log` and `.build/logs/ios/{doctor,build,verify}.log`.
- One invocation per platform may run at the same time. Multiple simultaneous builds of the same platform sharing output directories are not supported.

## Task 1: Build the independent Android shell

**Files:**

- Create: `.gitignore`, `.editorconfig`.
- Create: `apps/android/settings.gradle.kts`, `apps/android/build.gradle.kts`, `apps/android/gradle.properties`, `apps/android/gradle/libs.versions.toml`.
- Create: `apps/android/gradlew`, `apps/android/gradlew.bat`, `apps/android/gradle/wrapper/gradle-wrapper.jar`, `apps/android/gradle/wrapper/gradle-wrapper.properties`.
- Create: `apps/android/app/build.gradle.kts`, `apps/android/app/src/main/AndroidManifest.xml`.
- Create: `apps/android/app/src/main/java/com/example/nativetemplate/MainActivity.kt`.
- Create: `apps/android/app/src/main/res/values/strings.xml`, `apps/android/app/src/main/res/values/themes.xml`, `apps/android/app/src/main/res/drawable/ic_launcher.xml`.
- Create: `apps/android/README.md`, `apps/android/features/README.md`, `apps/android/core/README.md`, `apps/android/build-logic/README.md`.

**Interfaces:** Consumes the naming and platform minimums above. Produces a wrapper accepting `:app:assembleDebug`, `:app:lintDebug`, and `clean`, plus the Android artifact path. No iOS files are inputs.

- [ ] **1. Confirm execution baseline and record prerequisites.** Check Git status before edits. Use JDK 17 through command-scoped `JAVA_HOME`; locally it is available at `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`. Inspect `ANDROID_HOME` for `platforms/android-36/android.jar` and `build-tools/36.0.0`. These machine paths belong in the execution environment, not committed configuration. Resolve workspace isolation according to the user's existing preference and applicable execution workflow.

- [ ] **2. Add output exclusions and text conventions.** `.gitignore` must contain the following focused exclusions; do not ignore the wrapper JAR or the shared Xcode scheme:

```gitignore
.DS_Store
.build/
.worktrees/
.idea/
*.iml
apps/android/.gradle/
apps/android/.kotlin/
apps/android/**/build/
apps/android/local.properties
apps/ios/**/.build/
apps/ios/**/DerivedData/
apps/ios/**/xcuserdata/
*.xcuserstate
*.jks
*.keystore
*.p12
*.mobileprovision
__pycache__/
```

Use `.editorconfig` with `root = true`, UTF-8, LF, a final newline, trailing whitespace removal, and four-space indentation; specify CRLF for `*.bat`. No automatic formatter dependency is required for the shell.

- [ ] **3. Generate the genuine Gradle wrapper in a temporary bootstrap project.** Use the already installed Gradle 9.4.1 executable if available; otherwise obtain that official distribution and validate its published checksum. The local inspected executable is `$HOME/.gradle/wrapper/dists/gradle-9.4.1-bin/arn2x92ynaizyzdaamcbpbhtj/gradle-9.4.1/bin/gradle`. Assign its verified location to `NATIVE_GRADLE_BOOTSTRAP` for this command only.

```sh
NATIVE_WRAPPER_TMP=$(mktemp -d)
printf '%s\n' 'rootProject.name = "wrapper-bootstrap"' > "$NATIVE_WRAPPER_TMP/settings.gradle.kts"
curl --fail --location https://services.gradle.org/distributions/gradle-9.4.1-bin.zip.sha256 \
  --output "$NATIVE_WRAPPER_TMP/gradle.sha256"
NATIVE_GRADLE_SHA=$(cat "$NATIVE_WRAPPER_TMP/gradle.sha256")
printf '%s\n' "$NATIVE_GRADLE_SHA" | LC_ALL=C grep -Eq '^[a-f0-9]{64}$' || exit 1
"$NATIVE_GRADLE_BOOTSTRAP" -p "$NATIVE_WRAPPER_TMP" wrapper \
  --gradle-version 9.4.1 --distribution-type bin \
  --gradle-distribution-sha256-sum "$NATIVE_GRADLE_SHA"
mkdir -p apps/android/gradle/wrapper
cp "$NATIVE_WRAPPER_TMP/gradlew" "$NATIVE_WRAPPER_TMP/gradlew.bat" apps/android/
cp "$NATIVE_WRAPPER_TMP/gradle/wrapper/gradle-wrapper.jar" \
  "$NATIVE_WRAPPER_TMP/gradle/wrapper/gradle-wrapper.properties" apps/android/gradle/wrapper/
chmod +x apps/android/gradlew
```

Run these steps with failure checking enabled. Do not substitute a global-Gradle shim for a checked-in wrapper. Do not copy unrelated cached dependencies into the repository.

- [ ] **4. Configure the pinned Android project.** Use this initial stable baseline: AGP `9.2.1`, Gradle `9.4.1`, Kotlin/Compose compiler plugin `2.2.10`, Compose BOM `2026.02.01`, Activity Compose `1.12.4`, Java 17, compile/target SDK 36, build tools `36.0.0`, minimum SDK 26. AGP 9 supplies Kotlin support; do not also apply `org.jetbrains.kotlin.android`. The baseline is selected for compatibility, not as a claim that every dependency is the newest available release.

`gradle/libs.versions.toml`:

```toml
[versions]
agp = "9.2.1"
kotlin = "2.2.10"
compose-bom = "2026.02.01"
activity-compose = "1.12.4"

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-material3 = { module = "androidx.compose.material3:material3" }
activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

`settings.gradle.kts` uses `google()`, `mavenCentral()`, and `gradlePluginPortal()` for plugin management; dependency resolution uses only `google()` and `mavenCentral()` with `FAIL_ON_PROJECT_REPOS`. Set `rootProject.name = "NativeTemplate"` and `include(":app")`. The root build file declares the two catalog plugins with `apply false`. Set `android.useAndroidX=true` and `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8` in `gradle.properties`.

`app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.nativetemplate"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.example.nativetemplate"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
}
```

- [ ] **5. Add the empty native screen and required resources.** `MainActivity.kt`:

```kotlin
package com.example.nativetemplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.app_name))
                    }
                }
            }
        }
    }
}
```

Create `app_name` with value `NativeTemplate`. Define `Theme.NativeTemplate` inheriting `android:style/Theme.Material.Light.NoActionBar`, without an action bar. The manifest contains an application with this theme, label, `allowBackup="false"`, `supportsRtl="true"`, and drawable launcher icon. Its only activity is exported `.MainActivity` with the `MAIN` action and `LAUNCHER` category. Declare no permissions. Use a simple platform-native vector icon:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp" android:height="48dp"
    android:viewportWidth="48" android:viewportHeight="48">
    <path android:fillColor="#263238" android:pathData="M0,0h48v48h-48z" />
    <path android:fillColor="#FFFFFF" android:pathData="M12,12h24v24h-24z" />
</vector>
```

- [ ] **6. Run the independent native build and lint.** With valid command-scoped SDK/JDK settings:

```sh
cd apps/android
./gradlew --no-daemon :app:assembleDebug :app:lintDebug
test -s app/build/outputs/apk/debug/app-debug.apk
```

Expected: both Gradle tasks succeed; the APK exists; no Xcode command executes. If dependency metadata rejects the proposed baseline, investigate the actual diagnostic and adjust only the necessary pinned versions using official metadata, then record the verified change. Never suppress compatibility checks to obtain a green build.

- [ ] **7. Document and commit Android.** The platform README lists prerequisites, exact pinned versions, native commands, artifact/lint report paths, and rename locations. Extension READMEs explain when to add a feature/core Gradle module and why `build-logic/` has no convention plugin yet. Run `git diff --check`, stage only `.gitignore`, `.editorconfig`, and `apps/android/`, and commit as `feat(android): add empty native application`.

## Task 2: Build the independent iOS shell

**Files:**

- Create: `apps/ios/App/NativeTemplateApp.swift`, `apps/ios/App/ContentView.swift`.
- Create: `apps/ios/App/Resources/Assets.xcassets/Contents.json`.
- Create: `apps/ios/NativeTemplate.xcodeproj/project.pbxproj`, `apps/ios/NativeTemplate.xcodeproj/xcshareddata/xcschemes/NativeTemplate.xcscheme`.
- Create: `apps/ios/README.md`, `apps/ios/Packages/Features/README.md`, `apps/ios/Packages/Core/README.md`.

**Interfaces:** Consumes the approved bundle identifier, name, and iOS minimum. Produces Xcode project `NativeTemplate`, shared scheme `NativeTemplate`, and the simulator artifact contract. No Gradle or Android files are inputs.

- [ ] **1. Add the SwiftUI shell.** `NativeTemplateApp.swift`:

```swift
import SwiftUI

@main
struct NativeTemplateApp: App {
    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
```

`ContentView.swift`:

```swift
import SwiftUI

struct ContentView: View {
    var body: some View {
        Text("NativeTemplate")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(uiColor: .systemBackground))
    }
}
```

Create the asset catalog root `Contents.json` as `{"info":{"author":"xcode","version":1}}`. A store-ready app icon is deferred; do not configure a missing AppIcon set or invent a brand.

- [ ] **2. Create and commit a normal Xcode project with a shared scheme.** A one-time generator can use the already installed `xcodeproj` Ruby gem; it must not become a downstream build dependency or a checked-in bootstrap requirement. On this machine the gem is in `/opt/homebrew/opt/cocoapods/libexec` and Ruby is `/opt/homebrew/opt/ruby/bin/ruby`. Run this generator from `apps/ios/` with the gem environment scoped to that one invocation:

```ruby
require 'xcodeproj'

project = Xcodeproj::Project.new('NativeTemplate.xcodeproj')
target = project.new_target(:application, 'NativeTemplate', :ios, '17.0')
app_group = project.main_group.new_group('App', 'App')
%w[NativeTemplateApp.swift ContentView.swift].each do |name|
  target.add_file_references([app_group.new_file(name)])
end
assets = app_group.new_file('Resources/Assets.xcassets')
target.resources_build_phase.add_file_reference(assets)
target.build_configurations.each do |config|
  settings = config.build_settings
  settings['PRODUCT_BUNDLE_IDENTIFIER'] = 'com.example.nativetemplate'
  settings['PRODUCT_NAME'] = 'NativeTemplate'
  settings['SWIFT_VERSION'] = '6.0'
  settings['IPHONEOS_DEPLOYMENT_TARGET'] = '17.0'
  settings['TARGETED_DEVICE_FAMILY'] = '1,2'
  settings['GENERATE_INFOPLIST_FILE'] = 'YES'
  settings['INFOPLIST_KEY_CFBundleDisplayName'] = 'NativeTemplate'
  settings['INFOPLIST_KEY_UIApplicationSceneManifest_Generation'] = 'YES'
  settings['INFOPLIST_KEY_UILaunchScreen_Generation'] = 'YES'
  settings['INFOPLIST_KEY_UISupportedInterfaceOrientations'] =
    'UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight'
  settings['CODE_SIGN_STYLE'] = 'Automatic'
  settings.delete('DEVELOPMENT_TEAM')
  settings.delete('ASSETCATALOG_COMPILER_APPICON_NAME')
end
project.save
scheme = Xcodeproj::XCScheme.new
scheme.add_build_target(target)
scheme.set_launch_target(target)
scheme.save_as(project.path, 'NativeTemplate', true)
```

The resulting checked-in files, not Ruby, are the build inputs. Inspect them for absolute machine paths and remove unintended generator defaults such as references to nonexistent files. Xcode's project templates are a fallback if the local helper is unavailable; preserve the same one-target and shared-scheme contract.

- [ ] **3. Run a native simulator build without Android prerequisites.** From the repository root:

```sh
xcodebuild -list -project apps/ios/NativeTemplate.xcodeproj
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/ios CODE_SIGNING_ALLOWED=NO build
test -x .build/ios/Build/Products/Debug-iphonesimulator/NativeTemplate.app/NativeTemplate
test -s .build/ios/Build/Products/Debug-iphonesimulator/NativeTemplate.app/Info.plist
```

Expected: the shared scheme is listed and the application builds successfully. This proves simulator compilation, not launch. If the sandbox blocks Xcode services or cache writes, use the permitted execution mechanism for this authorized local build; do not change the application architecture to work around the sandbox.

- [ ] **4. Document and commit iOS.** Include Xcode prerequisites, project/scheme names, direct build command, output path, deployment minimum, and all rename locations. Package READMEs explain local Swift packages, target visibility, and the app → features → core dependency direction. Validate the project with `plutil -lint`, run `git diff --check`, stage `apps/ios/`, and commit as `feat(ios): add empty native application`.

## Task 3: Add reliable platform commands

**Files:** Create `tooling/scripts/doctor`, `tooling/scripts/build`, `tooling/scripts/verify`, `tooling/scripts/lib.sh`, and `tooling/tests/test_commands.py`.

**Interfaces:** Consumes the exact native commands and artifacts from Tasks 1 and 2. Produces the CLI, status, and log contracts above. `lib.sh` provides `native_dispatch(action, argv...)`, `native_doctor(platform)`, `native_build(platform)`, `native_verify(platform)`, and `native_logged(platform, action, command...)`. Shell return codes follow the public contract; functions do not return parsed JSON or mutate global toolchain selection.

- [ ] **1. Write black-box tests before the command implementation.** Use Python `unittest` and temporary directories. Copy `tooling/scripts/` into a fixture path whose name contains spaces when the source exists; otherwise create the fixture's empty script directory so setup succeeds before implementation. Create fake `apps/android/gradlew`, a nonempty `apps/android/gradle/wrapper/gradle-wrapper.jar`, `JAVA_HOME/bin/java`, `ANDROID_HOME/platforms/android-36/android.jar`, and `ANDROID_HOME/build-tools/36.0.0`. Put fake `uname`, `xcodebuild`, and `xcrun` first on the fixture PATH. Fake `uname` reports Darwin so these tests also work on Linux. The fake Java executable reports version 17 and the fake Xcode reports version 27. The iOS SDK returned by fake `xcrun` exists in the fixture.

The fixture's `run_cli(action, *arguments, **overrides)` runs `/bin/sh` with the script path and arguments from outside the copied repository using `subprocess.run([...], cwd=temp_directory, env=fixture_environment, text=True, capture_output=True)`. It returns the `CompletedProcess`; before implementation the missing script produces a nonzero shell status rather than a fixture setup exception. `artifact(platform)` returns the Android APK path or the iOS application executable path, as defined above. `trace()` reads the fake-tool trace as text, returning an empty string when it does not exist. Each test gets a new fixture.

Fake build tools append `android build` or `ios build` to the trace only on actual build/lint invocation, create the expected artifacts unless `NATIVE_FAKE_NO_ARTIFACT=1`, and exit with `NATIVE_FAKE_ANDROID_STATUS` or `NATIVE_FAKE_IOS_STATUS` (default zero). Fake iOS also writes an `Info.plist`. Prerequisite probes never create artifacts. These variables exist only in the fake tools, not production code.

Use these concrete test cases:

```python
def test_paths_with_spaces_from_another_directory(self):
    self.assertEqual(self.run_cli('build', 'android').returncode, 0)
    self.assertEqual(self.run_cli('build', 'ios').returncode, 0)
    self.assertTrue(self.artifact('android').exists())
    self.assertTrue(self.artifact('ios').exists())

def test_invalid_usage_dispatches_nothing(self):
    for args in [(), ('harmonyos',), ('android', 'extra'), ('--help',)]:
        result = self.run_cli('build', *args)
        self.assertEqual(result.returncode, 2)
        self.assertIn('Usage:', result.stderr)
    self.assertEqual(self.trace(), '')

def test_invalid_java_does_not_block_ios(self):
    result = self.run_cli('doctor', 'android', JAVA_HOME='/nonexistent/jdk')
    self.assertEqual(result.returncode, 1)
    self.assertIn('JAVA_HOME', result.stdout + result.stderr)
    self.assertEqual(self.run_cli('build', 'ios', JAVA_HOME='/nonexistent/jdk').returncode, 0)

def test_invalid_xcode_does_not_block_android(self):
    result = self.run_cli('doctor', 'ios', DEVELOPER_DIR='/nonexistent/xcode')
    self.assertEqual(result.returncode, 1)
    self.assertIn('DEVELOPER_DIR', result.stdout + result.stderr)
    self.assertEqual(self.run_cli('build', 'android', DEVELOPER_DIR='/nonexistent/xcode').returncode, 0)

def test_invalid_android_sdk_is_explicit(self):
    result = self.run_cli('doctor', 'android', ANDROID_HOME='/nonexistent/sdk')
    self.assertEqual(result.returncode, 1)
    self.assertIn('ANDROID_HOME', result.stdout + result.stderr)
    self.assertEqual(self.run_cli('build', 'ios', ANDROID_HOME='/nonexistent/sdk').returncode, 0)

def test_native_failure_is_not_hidden_by_stale_artifact(self):
    self.assertEqual(self.run_cli('build', 'android').returncode, 0)
    result = self.run_cli('verify', 'android', NATIVE_FAKE_ANDROID_STATUS='73')
    self.assertEqual(result.returncode, 73)
    self.assertTrue(self.artifact('android').exists())

def test_all_attempts_both_and_preserves_first_failure(self):
    result = self.run_cli('verify', 'all', NATIVE_FAKE_ANDROID_STATUS='73')
    self.assertEqual(result.returncode, 73)
    self.assertIn('android build', self.trace())
    self.assertIn('ios build', self.trace())
    self.assertTrue(self.artifact('ios').exists())

def test_success_without_artifact_fails_verification(self):
    for platform in ('android', 'ios'):
        result = self.run_cli('verify', platform, NATIVE_FAKE_NO_ARTIFACT='1')
        self.assertEqual(result.returncode, 1)
        self.assertIn('artifact', result.stdout + result.stderr)
```

The fixture methods are part of `CommandTests(unittest.TestCase)` and are implemented in the same test file. Use `TemporaryDirectory`, `Path`, `shutil.copytree`, and executable fake shell scripts; do not depend on a mocking library. Mark the fake iOS executable executable with `chmod(0o755)` so artifact validation is meaningful. Add an aggregate prerequisite-failure test confirming an available platform still runs, an iOS native-status test, and a `ThreadPoolExecutor` test running one fake build per platform and checking distinct log/artifact paths.

- [ ] **2. Run the new tests and confirm failure because the command entry points do not exist yet.** Run `python3 -m unittest discover -s tooling/tests -v`. Correct fixture setup failures unrelated to the missing implementation before continuing.

- [ ] **3. Implement small POSIX command entry points.** Each entry point loads `lib.sh` relative to itself and dispatches its fixed action; for example `build`:

```sh
#!/bin/sh
set -u
NATIVE_SCRIPT_DIR=$(CDPATH= cd -P "$(dirname "$0")" && pwd) || exit 1
NATIVE_ROOT=$(CDPATH= cd -P "$NATIVE_SCRIPT_DIR/../.." && pwd) || exit 1
. "$NATIVE_SCRIPT_DIR/lib.sh"
native_dispatch build "$@"
```

Use the same prelude with `native_dispatch doctor "$@"` and `native_dispatch verify "$@"` in the other two files. Set executable mode on the entry points. Quote all paths and arguments; do not use `eval` or parse a command string.

Argument validation runs before any native command. Dispatch each platform action in a subshell so function variables and working-directory changes do not leak into aggregate status tracking. The aggregation pattern is:

```sh
native_first_status=0
for native_platform in android ios; do
    if ( native_run_action "$native_action" "$native_platform" ); then
        printf '%s: passed\n' "$native_platform"
    else
        native_platform_status=$?
        printf '%s: failed (%s)\n' "$native_platform" "$native_platform_status" >&2
        if [ "$native_first_status" -eq 0 ]; then
            native_first_status=$native_platform_status
        fi
    fi
done
return "$native_first_status"
```

Define `native_run_action(action, platform)` as an internal selector for `native_doctor`, `native_build`, or `native_verify`; it accepts only the three fixed action names. Do not rely on `set -e` inside functions called as conditionals: explicitly preserve and return failures.

- [ ] **4. Implement platform-specific prerequisite checks.** Android requires `ANDROID_HOME` pointing to the pinned SDK/build-tools directories. If `JAVA_HOME` is provided, validate and use its Java executable without fallback; otherwise probe `java` on PATH and report a useful failure if it is a nonfunctional launcher. Check that its reported major version is 17 for this baseline and print the version. The error must explain how to set `JAVA_HOME`, not rewrite shell profiles. Check the Gradle wrapper JAR and executable before building.

iOS requires Darwin, a valid explicitly supplied `DEVELOPER_DIR` when present, successful `xcodebuild -version`, and successful `xcrun --sdk iphonesimulator --show-sdk-path`. Require that the returned SDK directory exists. It must not probe Android/Java. Both doctors print only relevant toolchain information and exact missing prerequisites. They do not install tools or accept SDK license agreements.

- [ ] **5. Invoke native builds with separate outputs and reliable logs.** Android runs in a subshell with working directory `apps/android/`; use `./gradlew --no-daemon :app:assembleDebug` for build and add `:app:lintDebug` for verify. iOS invokes the project, shared scheme, Debug configuration, generic simulator destination, derived data path, and `CODE_SIGNING_ALLOWED=NO` exactly as Task 2. Record output without losing the native exit code:

```sh
if "$@" > "$native_log" 2>&1; then
    native_command_status=0
else
    native_command_status=$?
fi
cat "$native_log"
printf 'Log: %s\n' "$native_log"
return "$native_command_status"
```

`native_logged` creates the action's platform log directory, runs this pattern in a subshell, and returns the captured status. Log-directory creation failure returns 1. Avoid an unguarded `command | tee` pipeline. For build and verify, include relevant preflight output in the action log, and return before artifact checks when the native process fails. On success, require the nonempty Android APK or the iOS executable plus nonempty plist and print the artifact path. A missing artifact returns 1 with a message containing `artifact`.

- [ ] **6. Run command tests and real verification.** Run `sh -n` on each of the four shell files and `python3 -m unittest discover -s tooling/tests -v`. Then run `./tooling/scripts/doctor all` and `./tooling/scripts/verify all` with the real JDK/SDK configuration. Expected: command tests pass, Android builds and lint passes, iOS builds, and both artifacts are reported. Capture any unperformed checks accurately.

- [ ] **7. Commit the verified command layer.** Run `git diff --check`, stage `tooling/scripts/` and `tooling/tests/`, and commit as `feat(tooling): add independent native build commands`.

## Task 4: Add contributor guidance and prove build isolation

**Files:**

- Create: `README.md`, `AGENTS.md`, `apps/android/AGENTS.md`, `apps/ios/AGENTS.md`.
- Create: `apps/harmonyos/README.md`, `contracts/README.md`, `docs/specs/README.md`.
- Create: `docs/architecture/overview.md`, `docs/architecture/toolchains.md`, `docs/decisions/0001-independent-native-projects.md`.
- Create: `docs/workflows/development.md`, `docs/workflows/verification.md`, `docs/workflows/verification-2026-09-20.md`.
- Create: `tooling/templates/feature-spec.md`, `tooling/templates/work-item.md`, `tooling/templates/decision.md`.
- Modify: the platform READMEs with final verified toolchain values if they changed during implementation.

**Interfaces:** Consumes the actual files, CLI contracts, and measured results from Tasks 1–3. Produces an onboarding path, scoped contributor instructions, reusable work records, and auditable acceptance evidence. No new application behavior is introduced.

- [ ] **1. Write discoverable contributor rules.** Root `AGENTS.md` links architecture, platform instructions, commands, and work templates. State the app → features/core → core dependency direction, explicit interfaces for feature coordination, module creation only for real responsibilities, scoped task ownership, and reporting unrun checks as unverified. Platform guidance adds Kotlin/Gradle and Swift/Xcode details without duplicating the shared policy. Keep routine authorized edits/checks autonomous; do not import a specific agent product's approval workflow into this template.

`README.md` includes a repository map, independent setup/build quickstarts, the three shared commands, artifact paths, links to platform READMEs, and HarmonyOS's planned status. `toolchains.md` records pinned versions and validated host/tool versions. The architecture decision states why runtime implementations remain independent and why feature/core directories initially contain guidance rather than dummy libraries. The contract/spec READMEs state what future content belongs there and require no invented product schemas.

- [ ] **2. Add lightweight work templates with explicit fields.** A feature spec has Purpose, User-visible behavior, Platform coverage, Shared contracts, Acceptance scenarios, and Verification. A work item has Objective, Authorized completion boundary, Scope/files, Affected platforms, Dependencies, Acceptance criteria, Commands, Results/evidence, and Handoff/blockers. An architecture decision has Context, Decision, Alternatives, Consequences, and Validation. These are reusable blank forms, not unfinished implementation documents. Add concise field instructions rather than made-up sample product requirements.

- [ ] **3. Verify each native project opens independently.** List the Xcode project/scheme using the native command and import the Gradle project in Android Studio when UI access is available. A command-line Gradle configuration/build verifies the build graph; if IDE import cannot be inspected, report that check as unverified rather than implying it occurred. Keep IDE-generated local settings ignored.

- [ ] **4. Verify clean-output build ordering.** First build with native clean actions so cleaning stays within each project's output directory:

```sh
(cd apps/android && ./gradlew --no-daemon clean)
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/ios CODE_SIGNING_ALLOWED=NO clean
./tooling/scripts/build android
./tooling/scripts/build ios
./tooling/scripts/build android
```

Expected: every command succeeds and both artifact contracts hold. Capture statuses rather than relying on the last command's status. No arbitrary recursive deletion of paths is needed.

- [ ] **5. Verify real concurrent builds from outside the repository.** Assign the absolute checkout to `NATIVE_REPO`, then run this shell fragment from a temporary working directory:

```sh
"$NATIVE_REPO/tooling/scripts/build" android &
NATIVE_ANDROID_PID=$!
"$NATIVE_REPO/tooling/scripts/build" ios &
NATIVE_IOS_PID=$!
NATIVE_ANDROID_STATUS=0
wait "$NATIVE_ANDROID_PID" || NATIVE_ANDROID_STATUS=$?
NATIVE_IOS_STATUS=0
wait "$NATIVE_IOS_PID" || NATIVE_IOS_STATUS=$?
printf 'android=%s ios=%s\n' "$NATIVE_ANDROID_STATUS" "$NATIVE_IOS_STATUS"
test "$NATIVE_ANDROID_STATUS" -eq 0 && test "$NATIVE_IOS_STATUS" -eq 0
```

Check both artifacts and their separate logs. The fake-tool tests cover paths with spaces; also invoke the real scripts through a path containing spaces if the checkout itself has none. Do not claim same-platform concurrent output safety.

- [ ] **6. Confirm build integrity and documented commands.** Record a hash inventory of tracked files before native verification and compare it afterward; use Python `hashlib.sha256` over `git ls-files -z` so uncommitted documentation edits can remain stable across the comparison. Run `git diff --check` and `git status --short` and confirm no unexpected tracked changes. Use `git check-ignore` to confirm Android build output, `.build/ios`, logs, `local.properties`, and IDE user data are ignored while the Gradle wrapper JAR/shared scheme remain tracked. Run final `verify all` and the command tests after any implementation fixes; do not rerun expensive builds after documentation-only edits.

- [ ] **7. Attempt launch smoke checks when runtimes are available.** Read `adb devices` and `xcrun simctl list devices available`. For an already available Android emulator, install the debug APK with `adb -s SERIAL install -r` and launch `com.example.nativetemplate/.MainActivity`; replace `SERIAL` with the observed emulator identifier. For an available iOS simulator, install the built app with `xcrun simctl install DEVICE_ID APP_PATH` and launch `com.example.nativetemplate`; use the observed device identifier and absolute artifact path. Inspect each neutral screen using available UI tools. Record compilation and launch results separately. Do not claim launch verification from process output alone when the screen was not inspected.

- [ ] **8. Write the verification record and perform final review.** `verification-2026-09-20.md` lists actual host/toolchain versions, command statuses, artifact paths, Android lint results, sequential/concurrent results, source-integrity checks, command-test results, IDE/launch results, and specific remaining limitations. Never prefill success. Cross-check every design acceptance criterion against this record. Review for accidental machine paths in build inputs, hidden cross-platform prerequisites, unused modules/dependencies, and signing information. Follow the selected execution workflow's independent review requirements and address substantive findings.

- [ ] **9. Commit the documentation and evidence.** Stage only the listed documentation/template files and any necessary reviewed fixes. Run `git diff --cached --check`, then commit as `docs: document native architecture and build verification`. Report the final branch/commit, how to build either platform, verified outcomes, and any incomplete acceptance checks. A missing required build must remain explicitly incomplete.

## Plan self-review and acceptance mapping

| Approved requirement | Owning task and evidence |
| --- | --- |
| Empty native projects, naming, minimums, resources | Tasks 1 and 2: real native build artifacts |
| Feature/core structure without speculative layers | Tasks 1, 2, and 4: extension READMEs and dependency rules |
| Pinned wrapper and dependency configuration | Task 1: official wrapper, checksum, version catalog, build |
| Native commands and independent prerequisites | Task 3: fake-tool tests and real verification |
| Separate outputs, sequential/concurrent builds | Task 4, steps 4–5: native logs and artifacts |
| Failure codes, invalid input, missing artifacts | Task 3: black-box command tests |
| Work from another directory and spaces in paths | Tasks 3 and 4: fixture tests and real absolute-path invocation |
| Build cleanliness and ignored machine outputs | Task 4, step 6: tracked-file hashes and Git checks |
| Human/agent workflows and reusable templates | Task 4, steps 1–2: linked guidance and work definitions |
| HarmonyOS extension point | Task 4: planned-platform README; Task 3: unsupported-platform test |
| Honest build, lint, IDE, and launch evidence | Task 4, steps 3 and 7–8: measured verification note |

No independent product subsystem needs its own plan: the two application shells and shared command layer form one build-isolation deliverable. The platform implementations can be reviewed independently, but the integration evidence requires both.

## Execution handoff

Recommend **native execution in this session**, followed by the execution workflow's independent final review. There are four tightly scoped tasks; the command layer and final evidence depend on both native shells, and per-task agent handoffs would add overhead. Subagent-driven execution remains an option if the user prefers independent implementation/review for each task.

The user must review this written plan and select the execution method before implementation under the invoked planning workflow. The already approved architecture does not need another design review.

## References for version and build decisions

- [AGP 9.2 compatibility and patch notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes): Gradle 9.4.1, build tools 36.0.0, JDK 17, and built-in Kotlin baseline.
- [Android Studio template update to Compose BOM 2026.02.01](https://android.googlesource.com/platform/tools/base/+/a38a61c0405f32c8f80175f825f8680ba07793b9).
- [Activity 1.12.4 release notes](https://developer.android.com/jetpack/androidx/releases/activity#1.12.4).
- [Official Gradle 9.4.1 distribution checksum](https://services.gradle.org/distributions/gradle-9.4.1-bin.zip.sha256): retrieve and validate during wrapper generation.

The cached AGP 9.2.1 POM inspected during planning declares Kotlin Gradle plugin 2.2.10. Actual successful builds, not documentation alone, establish the template's verified dependency baseline.

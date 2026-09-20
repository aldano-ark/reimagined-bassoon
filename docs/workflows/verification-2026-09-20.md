# Initial template verification — 2026-09-20

## Environment and versions

Verified on macOS 27.0, Apple Silicon. Android uses Gradle 9.4.1, AGP 9.2.1, Kotlin/Compose compiler 2.2.10, Compose BOM 2026.02.01, Activity Compose 1.12.4, Android SDK 36, and build tools 36.0.0.

The initial Android build/lint passed using JDK 17.0.20.1. After the user's preference for Android Studio's bundled runtime, build/lint also passed using JBR 25.0.3. Android Studio generated portable daemon criteria selecting Java 25; these project settings are preserved. Java bytecode remains targeted at 17. No global Java or Xcode selection was changed.

iOS was compiled with Xcode 27.0 (27A266a), Swift 6.4 in Swift 6 language mode, and the iOS Simulator 27.0 SDK. The deployment minimum is iOS 17.0.

## Measured results

| Check | Result | Evidence |
| --- | --- | --- |
| Independent Android build and lint | Passed, exit 0 | `:app:assembleDebug :app:lintDebug`; APK produced |
| Independent iOS build | Passed, exit 0 | Shared `NativeTemplate` scheme; simulator app produced |
| Xcode scheme discovery | Passed, exit 0 | `xcodebuild -list` reports the one application target and shared scheme |
| Shared `doctor all` | Passed, exit 0 | Both platform prerequisites reported independently |
| Shared `verify all` | Passed, exit 0 | Android APK/lint and iOS executable/plist validated |
| Command tests | Passed, 16 tests | Real shell scripts with controlled external-tool fixtures |
| Native clean actions | Passed, both exit 0 | Gradle `clean` and Xcode `clean`, each using its own project/output paths |
| Ordered builds | Passed, all exit 0 | Android → iOS → Android |
| Concurrent builds | Passed, both exit 0 | One Android and one iOS build launched together |
| Invocation from outside checkout | Passed | Absolute entry points invoked from a temporary directory |
| Paths containing spaces | Passed | Real commands invoked through a spaced symlink path; test fixtures also use actual spaced repository and SDK paths |
| Tracked-file integrity | Passed | SHA-256 comparison: 37 tracked/project-configuration files unchanged across the isolation builds |
| Output isolation | Passed | Distinct artifacts and logs; Git ignores outputs, caches, SDK locations, and IDE user state |
| Xcode project format | Passed | `plutil -lint` |
| Xcode IDE build | User-reported pass | User reported “XCode build success” after taking over computer use |
| Android Studio IDE sync/build | Awaiting user confirmation | Command-line Gradle configuration/build passes; IDE confirmation is recorded separately |
| Emulator/simulator launch | Unverified | Computer use handed to the user; no runtime success inferred from compilation |

## Commands and artifacts

Normal entry points:

```sh
./tooling/scripts/doctor all
./tooling/scripts/verify all
python3 -m unittest discover -s tooling/tests -v
```

For Android, `JAVA_HOME` was selected for each invocation and `ANDROID_HOME` pointed to the installed SDK. The test suite covers incompatible Java versions, invalid explicit SDK/JDK/Xcode paths, aggregate failures, preserved native exit codes with stale artifacts, missing artifacts after a zero exit status, lint invocation, separate concurrent outputs, and invalid arguments that dispatch no native tools.

Artifacts:

- `apps/android/app/build/outputs/apk/debug/app-debug.apk`
- `.build/ios/Build/Products/Debug-iphonesimulator/NativeTemplate.app`
- `apps/android/app/build/reports/lint-results-debug.html`
- `.build/logs/<platform>/<command>.log`

Logs and artifacts are local ignored outputs, not portable files checked into Git. Re-running commands replaces their logs. This document records observed results, while the commands provide reproducible checks.

## Warnings and limits

Android lint reports zero errors and eight warnings: seven newer SDK/tool/dependency notices and one Android backup-configuration notice. The pinned baseline is intentionally retained; this work does not claim the newest dependency set or store-release readiness. Define product data-transfer/backup policy before adding stored user data.

The generated Xcode project referenced a nonexistent AccentColor asset. That unused setting was removed, and subsequent iOS builds succeeded. Xcode also reports skipped App Intents metadata extraction because this empty app has no App Intents dependency.

No signed device/archive/store release, Windows/Linux host execution, minimum-OS runtime execution, or HarmonyOS build has been verified. Only launcher JDKs 17 and 25 were used for real Android builds; the helper's accepted 17–26 range follows Gradle's compatibility matrix rather than claiming all combinations were tested.

## Execution decisions

- Work stayed on `feat/native-template` in this dedicated checkout instead of adding a worktree. A separate checkout is still needed for conflicting concurrent edits.
- Native compilation/lint verifies the empty shells; behavior tests target the command layer. There are no automated application runtime tests yet.
- `.gitattributes` preserves Windows wrapper CRLF checkouts while normalizing Git text and treating the wrapper JAR as binary.
- Gradle launcher support was widened from a JDK-17-only check to the supported 17–26 range after verifying the bundled JBR 25. New runtime combinations need their own native verification.

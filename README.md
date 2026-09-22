# Native Mobile Template

A monorepo for one product with independent native Android and iOS applications. Each app demonstrates native dev/staging/production configuration and a dummy API request URL.

## Start here

| Platform | Open in the native IDE | Prerequisites |
| --- | --- | --- |
| Android | `apps/android/` in Android Studio | Compatible JDK, Android SDK 36, build tools 36.0.0 |
| iOS | `apps/ios/NativeTemplate.xcodeproj` in Xcode | macOS, Xcode with the iOS Simulator SDK |

Follow the [Android setup](apps/android/README.md) or [iOS setup](apps/ios/README.md). Android commands need a compatible Java launcher through `JAVA_HOME` or PATH, and `ANDROID_HOME`. Android Studio's bundled JDK 25 works with this Gradle version; you can scope its selection to this project while older projects keep JDK 17. Xcode selection can be scoped through `DEVELOPER_DIR`.

From the repository root:

```sh
./tooling/scripts/doctor android
./tooling/scripts/build android
./tooling/scripts/verify android

./tooling/scripts/doctor ios
./tooling/scripts/build ios
./tooling/scripts/verify ios
./tooling/scripts/test ios
```

For `doctor`, `build`, and `verify`, use `all` instead of a platform to run both. Each platform command checks only its own prerequisites. `all` attempts both and fails if either fails. These commands use POSIX shell on macOS/Linux; Android also retains the normal Windows Gradle wrapper.

`build` selects dev Debug. `verify android` builds, lints, and unit-tests all six app variants and executes the design library's Robolectric component tests. `verify ios` compiles app/unit/UI tests for all three Debug configurations, builds all three Release configurations, and checks each app's identity and configuration. `test ios` executes app and component tests on its own disposable simulator (dev by default; `test ios all` selects all three environments). The test runner requires Python 3.9+ and an installed iOS Simulator runtime; see the [iOS guide](apps/ios/README.md#execute-tests).

Build outputs:

- Android APK: `apps/android/app/build/outputs/apk/dev/debug/app-dev-debug.apk`
- iOS app: `.build/ios/Build/Products/Debug-Dev-iphonesimulator/NativeTemplate.app`
- Logs: `.build/logs/<platform>/<command>.log`
- iOS test runs: `.build/ios-tests/<run>/` (logs, `.xcresult` bundles, summary, and separate DerivedData)

The commands also work by absolute path from another working directory. One Android and one iOS build can run concurrently; concurrent builds of the same platform require separate checkouts/output directories. Native commands remain documented in the platform READMEs.

[GitHub Actions](docs/workflows/github-actions.md) runs independent tooling, Android, and iOS checks on pull requests and pushes to `main`, and retains test evidence for 14 days. The iOS job also executes `test ios all` on GitHub's Xcode 27 preview runner.

## Environment configuration

Android uses `apps/android/config/{dev,stg,prod}.properties`; iOS uses `apps/ios/Config/{Dev,Stg,Prod}.xcconfig`. The native IDE selects the flavour or scheme. All three app identities can coexist, and every environment supports Debug and Release builds. No `.env` files or root generation step are needed.

The example constructs a `GET /health` request from the selected base URL and displays it without sending traffic to the dummy endpoint. See the [environment design](docs/specs/2026-09-20-native-environments.md) and each platform README for editing/renaming instructions.

## Repository map

```text
apps/android/          Kotlin/Compose app and native design-system module
apps/ios/              Swift/SwiftUI app, design-system package, unit and UI tests
contracts/             Future shared schemas and behavioral fixtures
docs/architecture/     Boundaries and verified toolchains
docs/decisions/        Architecture decisions and their rationale
docs/design-system/    Component contracts, native differences, and previews
docs/workflows/        Development and verification guidance
docs/specs/            Behavior specifications and environment contract
tooling/scripts/       Platform commands
tooling/tests/         Command behavior tests
tooling/templates/     Feature specs, work items, and decisions
```

The design system is the first core library on each platform. Other feature/core directories contain extension guidance; create additional modules or packages when they have a real responsibility. The apps share behavioral requirements and conventions; their runtime implementations remain native.

The [native design libraries](docs/design-system/README.md) provide adaptive styling, content spacing, buttons, labeled text fields, status views, and IDE previews. Android uses Material 3; iOS uses native SwiftUI styles.

## Humans and agents

Read [AGENTS.md](AGENTS.md) for the repository map and contributor rules, then follow the relevant platform guidance. Both humans and agents use the same [development workflow](docs/workflows/development.md), [work-item template](tooling/templates/work-item.md), and [verification commands](docs/workflows/verification.md).

A task defines its scope and authorized completion boundary. Contributors implement within that scope, collect verification evidence, and report unrun checks explicitly. Agent scheduling and external integrations are chosen by the adopting project.

## Adopt the template

1. Create your product repository from this tree and configure its remote.
2. Rename the Android application and iOS target using their platform README checklists. Update shared command/test artifact paths when changing target names.
3. Write the first feature's behavior in `docs/specs/`, including applicable platforms and acceptance criteria.
4. Add feature modules/packages as needed and keep dependency direction explicit.
5. Replace the dummy endpoints in both native configuration sets, review the included GitHub Actions workflow for your repository, and add signing and store assets when the product needs them.

See the [architecture](docs/architecture/overview.md), [toolchain baseline](docs/architecture/toolchains.md), and [measured verification record](docs/workflows/verification-2026-09-20.md). This template does not select a license for your product; choose one appropriate to its use and preserve third-party notices such as the Gradle wrapper's license headers.

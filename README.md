# Native Mobile Template

A monorepo for one product with independent native Android and iOS applications, plus a planned HarmonyOS integration point. Each app currently displays only `NativeTemplate`.

## Start here

| Platform | Open in the native IDE | Prerequisites |
| --- | --- | --- |
| Android | `apps/android/` in Android Studio | Compatible JDK, Android SDK 36, build tools 36.0.0 |
| iOS | `apps/ios/NativeTemplate.xcodeproj` in Xcode | macOS, Xcode with the iOS Simulator SDK |
| HarmonyOS | Documentation only | Defined when native support is added |

Follow the [Android setup](apps/android/README.md) or [iOS setup](apps/ios/README.md). Android commands need a compatible Java launcher through `JAVA_HOME` or PATH, and `ANDROID_HOME`. Android Studio's bundled JDK 25 works with this Gradle version; you can scope its selection to this project while older projects keep JDK 17. Xcode selection can be scoped through `DEVELOPER_DIR`.

From the repository root:

```sh
./tooling/scripts/doctor android
./tooling/scripts/build android
./tooling/scripts/verify android

./tooling/scripts/doctor ios
./tooling/scripts/build ios
./tooling/scripts/verify ios
```

Use `all` instead of a platform to run both. Each platform command checks only its own prerequisites. `all` attempts both and fails if either fails; HarmonyOS is excluded until implemented. The shared commands use POSIX shell on macOS/Linux; Android also retains the normal Windows Gradle wrapper.

`verify android` builds and runs Android lint. `verify ios` compiles for the generic iOS Simulator destination and checks the app artifact. These commands do not launch apps or claim runtime test coverage.

Build outputs:

- Android APK: `apps/android/app/build/outputs/apk/debug/app-debug.apk`
- iOS app: `.build/ios/Build/Products/Debug-iphonesimulator/NativeTemplate.app`
- Logs: `.build/logs/<platform>/<command>.log`

The commands also work by absolute path from another working directory. One Android and one iOS build can run concurrently; concurrent builds of the same platform require separate checkouts/output directories. Native commands remain documented in the platform READMEs.

## Repository map

```text
apps/android/          Kotlin/Compose app; future feature/core modules
apps/ios/              Swift/SwiftUI app; future local Swift packages
apps/harmonyos/        Planned platform boundary
contracts/             Future shared schemas and behavioral fixtures
docs/architecture/     Boundaries and verified toolchains
docs/decisions/        Architecture decisions and their rationale
docs/workflows/        Development and verification guidance
docs/specs/            Future product behavior specifications
tooling/scripts/       Platform commands
tooling/tests/         Command behavior tests
tooling/templates/     Feature specs, work items, and decisions
```

Feature/core directories contain extension guidance. Create compiled modules or packages when they have a real responsibility. The apps share behavioral requirements and conventions; their runtime implementations remain native.

## Humans and agents

Read [AGENTS.md](AGENTS.md) for the repository map and contributor rules, then follow the relevant platform guidance. Both humans and agents use the same [development workflow](docs/workflows/development.md), [work-item template](tooling/templates/work-item.md), and [verification commands](docs/workflows/verification.md).

A task defines its scope and authorized completion boundary. Contributors implement within that scope, collect verification evidence, and report unrun checks explicitly. Agent scheduling and external integrations are chosen by the adopting project.

## Adopt the template

1. Create your product repository from this tree and configure its remote.
2. Rename the Android application and iOS target using their platform README checklists. Update shared command/test artifact paths when changing target names.
3. Write the first feature's behavior in `docs/specs/`, including applicable platforms and acceptance criteria.
4. Add feature modules/packages as needed and keep dependency direction explicit.
5. Add CI, signing, release environments, and store assets when the product needs them.

See the [architecture](docs/architecture/overview.md), [toolchain baseline](docs/architecture/toolchains.md), and [measured verification record](docs/workflows/verification-2026-09-20.md). This template does not select a license for your product; choose one appropriate to its use and preserve third-party notices such as the Gradle wrapper's license headers.

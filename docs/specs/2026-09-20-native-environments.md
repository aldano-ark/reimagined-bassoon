# Native environment configuration

Status: proposed design for review. Native configuration and separate app identities are agreed; the implementation details below are proposed.

## Purpose

Let developers and testers build and install dev, staging, and production editions of this template on the same device. Demonstrate how a selected environment supplies an API endpoint to application code while keeping Android and iOS independently buildable.

The user selected native configuration files and explicitly excluded `.env` files. Both platforms use the canonical environment names `dev`, `stg`, and `prod`. Environment selection happens at build time.

## User-visible behavior

| Environment | Application / bundle identifier | Display name | Dummy API base URL |
| --- | --- | --- | --- |
| `dev` | `com.example.nativetemplate.dev` | NativeTemplate Dev | `https://dev-api.example.com` |
| `stg` | `com.example.nativetemplate.stg` | NativeTemplate Stg | `https://stg-api.example.com` |
| `prod` | `com.example.nativetemplate` | NativeTemplate | `https://api.example.com` |

All three identities can coexist on a device. Debug and Release builds of the same environment share its identity; there is no additional debug suffix. Installing a replacement still follows native platform signing rules.

The template's neutral screen becomes a small configuration demonstration. It displays the app name, selected environment, API base URL, and a `GET <base URL>/health` request constructed by application code. Long URLs wrap and remain readable with accessibility text sizes. The example uses the existing design library's theme and spacing, with native text controls.

The example constructs a request but does not execute it or display an invented server response. These URLs are placeholders. There is no environment switcher, network permission requirement, live service, or new networking dependency in this demonstration.

## Platform coverage

Both native applications and their build documentation are affected. Existing design-system components and their behavior remain intact.

### Android

- Add an `environment` flavour dimension with `dev`, `stg`, and `prod` product flavours. Keep `debug` and `release` build types, producing six app variants.
- Store checked-in values in `apps/android/config/dev.properties`, `stg.properties`, and `prod.properties`.
- Each environment file defines its canonical environment, full application identifier, display name, and API base URL. Gradle reads these files directly with the JDK properties API.
- Apply the application identifier and display-name resource for each flavour. Enable BuildConfig generation and expose the environment and API base URL to the app through generated constants.
- Keep the Kotlin namespace and source package stable. The design-system library does not gain environment flavours.
- Validate required values and their types during Gradle configuration. Diagnostics identify the file and key; missing configuration cannot select another environment implicitly.

### iOS

- Keep one application target and checked-in Xcode project. Add three shared schemes: `NativeTemplate-Dev`, `NativeTemplate-Stg`, and `NativeTemplate-Prod`.
- Give the project and app/test targets matching configurations: `Debug-Dev`, `Release-Dev`, `Debug-Stg`, `Release-Stg`, `Debug-Prod`, and `Release-Prod`.
- Each scheme uses its Debug configuration for Run, Test, and Analyze, and its Release configuration for Profile and Archive.
- Store settings under `apps/ios/Config/`, with one environment file per environment and shared Debug/Release settings composed by configuration-specific `.xcconfig` files.
- Environment settings define the canonical environment, bundle identifier, display name, and API base URL. Preserve inherited compiler settings; define `DEBUG` only in Debug configurations so the existing UI-test fixture stays excluded from Release builds.
- Use an explicit app Info.plist for environment and endpoint keys expanded from build settings. Remove conflicting project-level overrides when moving settings into `.xcconfig` files.
- Encode URL values using valid xcconfig syntax so `//` is not interpreted as a comment. Inspect the resolved plist to verify that the complete HTTPS URL reaches the application.
- Read and validate resolved values once at app startup. A missing or invalid value produces a configuration error state, with no request construction and no fallback endpoint. Tests cover this behavior.
- Keep matching test-host settings and distinct UI-test bundle identifiers for the three environments. Update documentation and shared commands to the new scheme names; replace the old `NativeTemplate` scheme.

## Configuration and request flow

```text
Native flavour / scheme
    -> platform-owned configuration file
    -> generated BuildConfig / resolved Info.plist
    -> validated, immutable AppConfig
    -> ApiRequestFactory receives API base URL
    -> GET /health request
    -> template view receives display values
```

Use small app-owned types for `AppEnvironment`, `AppConfig`, and `ApiRequestFactory`. App startup assembles dependencies. The request factory receives a base URL explicitly; it does not read BuildConfig, Bundle, files, or global environment state itself. The view receives the configuration and constructed request details.

The Kotlin and Swift types can differ. Use existing native/JDK URL facilities, with a lightweight method-and-URL value on Android and URLRequest on iOS. This responsibility does not require a new core module, Swift package, repository, or data layer.

Configuration validation accepts only the three canonical environment names and nonempty app identifiers/display names. API base URLs must be absolute HTTPS URLs with a host and without embedded credentials, a query, or a fragment. A base path is allowed; appending `health` must preserve that path and handle a trailing slash consistently. Build/test checks verify the expected environment-to-identity mapping.

Checked-in files contain the public configuration needed by a fresh checkout. No root generator, `.env` parser, Node.js, or other platform's toolchain is needed. Editing a value requires rebuilding the affected app. Configuration values are part of the built application, so credentials and signing material remain outside these files.

## Shared contracts and alternatives

The table above is the language-neutral behavioral contract. No runtime code or generated settings are shared across platforms. Matching endpoint values are maintained in both native configurations and checked for parity by the shared tooling test suite; that suite is verification, not a dependency of native compilation.

The chosen native approach keeps IDE builds direct and independent, at the cost of maintaining equivalent configuration values twice. Shared `.env` files were considered and rejected by the user. Runtime environment switching was excluded when separate build identities were selected.

## Build and verification workflow

Keep the existing single-platform-argument interface for `doctor`, `build`, and `verify`.

- `build android` and `build ios` select dev Debug. The documentation provides native Gradle and Xcode commands for selecting another environment or build mode.
- Android verification assembles and lints all six app variants, runs app configuration/request tests for all three Debug flavours, and retains the design-system lint and component-test checks once per invocation.
- iOS verification performs `build-for-testing` for all three Debug configurations and builds all three Release configurations for a generic simulator. Test compilation remains distinct from execution.
- Check each resulting artifact's application/bundle identity, name, and environment configuration through the appropriate native metadata, configuration tests, and resolved plist checks. Check expected artifact paths for every variant rather than accepting an earlier single Debug artifact.
- Preserve per-platform logs, first-failure propagation, independent prerequisites, and build output isolation. No build rewrites tracked configuration files or globally selects an SDK.
- Add meaningful Android app unit tests and an iOS app unit-test target for validation and request construction. Execute iOS app tests separately on a dedicated simulator and compile them through the shared verification command.
- Run existing iOS design-system UI tests separately on a dedicated simulator to verify that scheme/configuration changes preserve the test host.

## Acceptance scenarios

1. From a fresh checkout, either native project builds using its own IDE or command-line tools without invoking the other platform or a configuration-generation command.
2. Dev, staging, and production apps install together on an Android device/emulator and an iOS device/simulator, with the expected names and identifiers. Signing-dependent device evidence is separate from unsigned simulator evidence.
3. Launching each environment shows its own endpoint and a request to that endpoint's `/health` path. No outgoing request is made by the example.
4. Changing one native environment's configured endpoint and rebuilding that environment changes the constructed request; no request factory or view source change is required.
5. A Release staging build still uses the staging identifier and endpoint. Release does not imply production, and Debug does not imply dev.
6. Invalid or missing configuration fails explicitly during Android configuration or produces the defined iOS startup error. It never falls back to production or another environment.
7. Request-construction tests check HTTP method, environment-specific URLs, preserved base paths, and trailing-slash handling. Tests require no external service.
8. Debug iOS configurations retain access to the design-system test fixture; Release configurations exclude it. Existing component tests still pass where executed.
9. Shared command tests cover variant dispatch, artifact validation, failure propagation, and native configuration parity. Invalid command arguments retain their existing behavior.

## Affected files and documentation

- Android app build file, new native configuration files, app configuration/request example and tests, and Android README. Reuse the existing version-catalog JUnit entry for app tests.
- iOS Xcode project, schemes, configuration files, Info.plist, app configuration/request example and tests, and iOS README.
- Shared build/verification implementation and command tests; development and verification documentation, including exact variant commands and evidence limits.
- Record the environment-selection decision in `docs/decisions/` alongside implementation. Update rename instructions to cover all environment identifiers.

## Required verification and evidence

Run and record actual results for:

```sh
./tooling/scripts/doctor android
./tooling/scripts/doctor ios
python3 -m unittest discover -s tooling/tests -v
./tooling/scripts/verify android
./tooling/scripts/verify ios
```

Also execute iOS app tests and existing component UI tests on a dedicated simulator, inspect independent IDE builds, and install/launch all three identities on available native runtimes. Perform the shared-build isolation checks in `docs/workflows/verification.md` after changing shared build dispatch. Confirm generated output remains ignored and native builds leave tracked sources unchanged.

Record compilation, lint, executed tests, IDE import/build, and simultaneous installation/launch as separate evidence. Missing tools or unrun checks remain unverified. Simulator builds do not establish device signing, release archives, distribution, or store readiness.

This document records a design only. No application, build, or test changes have been implemented or verified as part of writing it.

## Native references

- [Android product flavours and build variants](https://developer.android.com/build/build-variants).
- [Apple build configuration files and setting precedence](https://developer.apple.com/documentation/xcode/adding-a-build-configuration-file-to-your-project).

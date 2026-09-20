# Verification

From the repository root:

```sh
./tooling/scripts/doctor android
./tooling/scripts/verify android
./tooling/scripts/doctor ios
./tooling/scripts/verify ios
./tooling/scripts/verify all
```

`doctor` reports prerequisites and versions; it does not install tools. Android accepts a Gradle-compatible launcher JDK (17–26) and requires an explicit `ANDROID_HOME` with platform 36/build tools 36.0.0. Android Studio's bundled JBR 25 can be selected for this project without changing older projects. Gradle daemon JVM criteria, when present, control the actual build runtime independently of the launcher. iOS uses the selected Xcode's simulator SDK. An invalid explicit toolchain path fails rather than silently choosing another installation.

Android verification builds, lints, and executes app unit tests for all six `dev`/`stg`/`prod` × Debug/Release variants. It also runs design-system lint and component tests once. APK metadata checks verify each identity and display name; app tests verify the compiled endpoint/configuration. Android doctor also requires the SDK's executable `aapt2`.

iOS verification runs `build-for-testing` for `Debug-Dev`, `Debug-Stg`, and `Debug-Prod`, then builds each matching Release configuration. It validates the executable and resolved plist identity, name, environment, and full API URL. Test targets compile only in Debug verification. iOS doctor requires `plutil` for metadata inspection. A native failure remains a failure even if an earlier artifact exists.

Execute iOS app and component tests separately on a dedicated simulator using the [iOS guide](../../apps/ios/README.md#execute-tests). Compilation is not an executed XCTest pass. Use the native [preview galleries](../design-system/README.md#previews) for component visual inspection and record observations separately.

`build android` and `build ios` select dev Debug. Native Gradle tasks and Xcode schemes select other environments and modes; see the platform READMEs. `verify` checks the complete matrix while retaining the existing one-platform-argument interface.

## Command behavior

- Exactly one platform argument is required: `android`, `ios`, or `all`.
- Exit 2 means invalid usage; exit 1 means a prerequisite or artifact error. Native process failures retain their exit code.
- `all` attempts both platforms and returns the first failure, if any.
- Logs are `.build/logs/<platform>/<command>.log`; each new run replaces that command's previous log. Preserve logs elsewhere when a task needs a lasting history.
- Each platform's prerequisites and artifacts are independent. One build per platform may run concurrently. Shared commands require a POSIX shell; Windows Android developers can use the native Gradle wrapper directly.

## Tooling tests

```sh
python3 -m unittest discover -s tooling/tests -v
```

The tests execute the real shell scripts with fake native tools. They verify argument/path handling, independent prerequisites, error propagation, variant dispatch, missing/mismatched artifact metadata, app/library test failures, iOS test compilation, aggregate results, and distinct concurrent outputs. They also check native configuration parity and shared scheme action mappings. They do not prove the native applications compile or their components behave correctly; run native verification and the applicable component suites separately.

## Isolation checks for build changes

1. Run native clean actions and build Android, then iOS, then Android again.
2. Invoke one Android and one iOS build concurrently from another working directory using absolute script paths.
3. Check both artifacts, per-platform logs, native statuses, and tracked-file hashes/Git status.
4. Confirm generated outputs and machine configuration remain ignored.

When runtimes are available, install and launch the apps and inspect each environment screen and confirm all three identities coexist. Record IDE import and runtime results separately. A successful build does not prove a launch, a device-signing configuration, a release archive, or store readiness.

See [the measured initial verification](verification-2026-09-20.md) and [environment verification](native-environments-verification-2026-09-21.md).

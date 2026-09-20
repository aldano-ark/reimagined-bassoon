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

Android verification assembles the debug APK and runs Android lint. iOS verification compiles the shared scheme for a generic simulator and validates its executable and plist. A native build failure remains a failure even if an older artifact exists.

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

The tests execute the real shell scripts with fake native tools. They verify argument/path handling, independent prerequisites, error propagation, artifact validation, lint dispatch, aggregate results, and distinct concurrent outputs. They do not prove the native applications compile; run native verification separately.

## Isolation checks for build changes

1. Run native clean actions and build Android, then iOS, then Android again.
2. Invoke one Android and one iOS build concurrently from another working directory using absolute script paths.
3. Check both artifacts, per-platform logs, native statuses, and tracked-file hashes/Git status.
4. Confirm generated outputs and machine configuration remain ignored.

When runtimes are available, install and launch the apps and inspect their neutral screens. Record IDE import and runtime results separately. A successful build does not prove a launch, a device-signing configuration, a release archive, or store readiness.

See [the measured initial verification](verification-2026-09-20.md).

# Android

An independent Kotlin/Jetpack Compose application. Open this directory in Android Studio; it contains the Gradle settings and wrapper. No iOS tools are required.

## Prerequisites

- A Gradle-compatible Java launcher (17–26 for Gradle 9.4.1). Use Android Studio's bundled JBR 25 for this project. The current daemon criteria require a Java 25 runtime even if a JDK 17 launcher starts Gradle. The app's Java bytecode target stays at 17 independently of the build runtime.
- Android SDK. Set `ANDROID_HOME` to the SDK directory and install platform `android-36` and build tools `36.0.0` using Android Studio's SDK Manager.
- Network access for the first Gradle/dependency download. SDK licenses must already be accepted through the normal SDK setup.

Machine-specific SDK locations may be stored in ignored `local.properties` for native IDE use. The shared command scripts require `ANDROID_HOME` explicitly.

## Choose a project JDK

Use Android Studio's bundled JDK in this project's Gradle settings. On macOS its standard location is `/Applications/Android Studio.app/Contents/jbr/Contents/Home`. Keep older projects' JDK selections unchanged.

For a terminal command, select the bundled JDK just for that invocation:

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./tooling/scripts/verify android
```

Run that example from the repository root with `ANDROID_HOME` already set. It does not change your shell's global `JAVA_HOME`.

Recent Android Studio versions can generate `gradle/gradle-daemon-jvm.properties`. When present, its JVM criteria control the build daemon and take precedence over `JAVA_HOME`; `./gradlew --version` shows launcher and daemon selections separately. Treat this as project configuration and review it before sharing it. It currently selects Java 25 in this working checkout.

## Environments and builds

Public configuration lives in `config/dev.properties`, `config/stg.properties`, and `config/prod.properties`. Each defines `APP_ENVIRONMENT`, `APP_ID`, `APP_DISPLAY_NAME`, and `API_BASE_URL`. Use one literal `KEY=value` per line, with no interpolation or inline comments. API endpoints must be absolute HTTPS URLs without credentials, query strings, or fragments. Invalid or missing values stop Gradle configuration with the file and key.

| Flavour | Application ID | Display name |
| --- | --- | --- |
| dev | com.example.nativetemplate.dev | NativeTemplate Dev |
| stg | com.example.nativetemplate.stg | NativeTemplate Stg |
| prod | com.example.nativetemplate | NativeTemplate |

Select a flavour and Debug/Release independently in Android Studio's Build Variants panel. All three environments install together. Debug and Release of the same environment share an identity and remain subject to Android signing rules.

From this directory:

```sh
./gradlew --no-daemon :app:assembleDevDebug
./gradlew --no-daemon :app:assembleStgRelease
./gradlew --no-daemon :app:lintStgRelease :app:testStgReleaseUnitTest
```

The root `./tooling/scripts/build android` selects dev Debug. `./tooling/scripts/verify android` builds, lints, and tests all six app variants, then runs the design-library lint and component tests once.

Debug APKs: `app/build/outputs/apk/<env>/debug/app-<env>-debug.apk`.
Unsigned Release APKs: `app/build/outputs/apk/<env>/release/app-<env>-release-unsigned.apk`.
Lint reports: `app/build/reports/lint-results-<env><Mode>.html`.

The template screen displays the selected environment and dummy API endpoint, then a `GET /health` request constructed by `ApiRequestFactory` from the validated `AppConfig`. It sends no network request. Change an endpoint in its native configuration and rebuild; a base path such as `/v2/` is retained in `/v2/health`. Keep the corresponding iOS configuration in sync; the shared Python suite checks parity without participating in native compilation.

Configuration is embedded in the application. Keep signing material and credentials outside these files. No `.env` file or root generation command is required.

## Baseline

AGP 9.2.1; Gradle 9.4.1; Kotlin/Compose compiler 2.2.10; Compose BOM 2026.02.01; Activity Compose 1.12.4. AGP supplies Kotlin support. Versions are pinned in `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`.

Minimum Android API: 26. Compile/target SDK: 36. Java compilation: 17.

## Rename for a product

1. Change `rootProject.name` in `settings.gradle.kts`.
2. Change `namespace` and the base `applicationId` in `app/build.gradle.kts`, and all three `APP_ID` entries in `config/*.properties`. Update the identity contract in app/shared tests.
3. Rename the Kotlin package and corresponding source directory; update any fully qualified manifest references if introduced later.
4. Change `APP_DISPLAY_NAME` in all three configuration files and replace the launcher icon. Gradle generates the `app_name` resource.
5. Update the iOS identifier/name independently and update shared tooling artifact checks if native target or artifact names change.

See [features](features/README.md), [core](core/README.md), and [build logic](build-logic/README.md) before adding modules.

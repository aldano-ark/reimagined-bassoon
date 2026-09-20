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

## Build

From this directory:

```sh
./gradlew --no-daemon :app:assembleDebug
./gradlew --no-daemon :app:lintDebug
```

From the repository root, the equivalent entry points are `./tooling/scripts/build android` and `./tooling/scripts/verify android`. Verification builds and runs Android lint.

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Lint report: `app/build/reports/lint-results-debug.html`.

## Baseline

AGP 9.2.1; Gradle 9.4.1; Kotlin/Compose compiler 2.2.10; Compose BOM 2026.02.01; Activity Compose 1.12.4. AGP supplies Kotlin support. Versions are pinned in `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`.

Minimum Android API: 26. Compile/target SDK: 36. Java compilation: 17.

## Rename for a product

1. Change `rootProject.name` in `settings.gradle.kts`.
2. Change `namespace` and `applicationId` in `app/build.gradle.kts`.
3. Rename the Kotlin package and corresponding source directory; update any fully qualified manifest references if introduced later.
4. Change `app_name` in `app/src/main/res/values/strings.xml` and replace the launcher icon.
5. Update the iOS identifier/name independently and update shared tooling artifact checks if native target or artifact names change.

See [features](features/README.md), [core](core/README.md), and [build logic](build-logic/README.md) before adding modules.

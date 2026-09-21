# Toolchain baseline

| Component | Pinned or validated version | Source of configuration |
| --- | --- | --- |
| Android minimum SDK | 26 | `apps/android/app/build.gradle.kts` |
| Android compile/target SDK | 36 | `apps/android/app/build.gradle.kts` |
| Android build tools | 36.0.0 | `apps/android/app/build.gradle.kts` |
| Gradle launcher JDK | Supported range 17–26; initial build validated with 17.0.20.1 | `JAVA_HOME` or PATH |
| Bundled Android Studio JDK | Build/lint validated with JBR 25.0.3 | Project Gradle settings or command-scoped `JAVA_HOME` |
| Java bytecode target | 17 | Android app `compileOptions` |
| Gradle | 9.4.1 | Wrapper properties and SHA-256 checksum |
| Android Gradle plugin | 9.2.1 | Android version catalog |
| Kotlin / Compose compiler | 2.2.10 | AGP built-in Kotlin / Android version catalog |
| Compose BOM | 2026.02.01 | Android version catalog |
| Activity Compose | 1.12.4 | Android version catalog |
| iOS deployment minimum | 17.0 | Xcode project |
| Xcode | Validated with 27.0, build 27A266a | Installed Xcode / `DEVELOPER_DIR` |
| Swift | Validated with 6.4; language mode 6 | Xcode toolchain / project |
| iOS Simulator SDK | Validated with 27.0 | Selected Xcode |

These are the verified baseline versions, not a promise that arbitrary combinations work. Native builds require neither a monorepo framework nor Node.js/Ruby/Python. The command test suite uses Python 3's standard library separately from native builds. The optional `test ios` runner requires Python 3.9+ and an available iOS Simulator runtime; it uses the standard library without package installation.

Keep exact dependency versions and the official Gradle wrapper checked in. For upgrades, consult the [AGP compatibility table](https://developer.android.com/build/releases/agp-9-2-0-release-notes), [Gradle Java compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html), [Kotlin support guidance](https://developer.android.com/build/migrate-to-built-in-kotlin), and [Compose setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), then run both affected native checks. Update the Android doctor prerequisites with SDK/JDK baseline changes.

The app's Java 17 bytecode target is independent of the Gradle runtime. Project-scoped Gradle daemon JVM criteria, when present in `apps/android/gradle/gradle-daemon-jvm.properties`, take precedence over launcher environment settings. This permits a modern runtime for this template while legacy projects keep their own JDK configuration. The current IDE-generated criteria select Java 25.

Xcode is externally installed; the repository does not install it or change `xcode-select`. Record the actual selected version in verification evidence. Test the project on other Xcode versions before claiming their support.

No personal SDK locations, development teams, signing certificates, or user IDE state belong in tracked files.

# Native mobile monorepo template

Date: 2026-09-20

Status: Implemented and reviewed on 2026-09-20. Both native shells, shared commands, and build isolation are verified; both IDE builds are user-confirmed. Runtime screen checks remain unverified after computer control was handed to the user.

Execution update: The user prefers Android Studio's bundled JDK for this new template while retaining JDK 17 for older projects. Gradle 9.4.1 supports launcher JDKs 17–26; the bundled JBR 25.0.3 passed Android build/lint verification. Android Studio generated project daemon criteria selecting Java 25. Keep runtime selection project-scoped and retain Java 17 bytecode compatibility. This supersedes the implementation plan's original JDK-17-only prerequisite.

## Intent and agreed scope

Create a reusable repository template for one product implemented independently on Android in Kotlin and iOS in Swift. Reserve a clear integration point for HarmonyOS, whose implementation comes later. There is no product domain or sample product to implement.

The template must support humans and AI agents working together, and agents executing well-defined work autonomously. Contributors should be able to discover conventions, identify the affected platform, make a scoped change, run the appropriate checks, and leave reviewable evidence.

The user selected a feature-oriented native foundation: more guidance than a pair of bare platform folders, without prescribing a complete stack or creating speculative architecture layers. Include buildable, empty Android and iOS applications to verify that the two build systems coexist and operate independently.

## Proposed defaults

These are concrete design choices for review, rather than additional requirements supplied by the user:

- Use Jetpack Compose for Android and SwiftUI for iOS.
- Support Android API 26 and later, and iOS 17 and later, as initial deployment minimums. These are independent of the installed compile SDK versions.
- Use `NativeTemplate` as the temporary application name and `com.example.nativetemplate` as the application/bundle identifier. Document all rename locations.
- Keep a normal Gradle project and a committed Xcode project with a shared scheme. Opening either platform in its native IDE requires no project-generation step.
- Use POSIX shell entry points for shared commands. Native builds do not depend on Node.js, Ruby, a monorepo framework, or the other platform's SDK.
- Provide platform-neutral verification commands that a later CI provider can invoke. Hosted CI configuration and remote repository setup are outside this initial slice.

## Repository structure

```text
/
├── README.md
├── AGENTS.md
├── .gitignore
├── .editorconfig
├── apps/
│   ├── android/
│   │   ├── README.md
│   │   ├── AGENTS.md
│   │   ├── app/
│   │   ├── features/README.md
│   │   ├── core/README.md
│   │   ├── build-logic/README.md
│   │   ├── gradle/
│   │   ├── gradlew
│   │   ├── gradlew.bat
│   │   ├── settings.gradle.kts
│   │   ├── build.gradle.kts
│   │   └── gradle.properties
│   ├── ios/
│   │   ├── README.md
│   │   ├── AGENTS.md
│   │   ├── NativeTemplate.xcodeproj/
│   │   ├── App/
│   │   └── Packages/
│   │       ├── Features/README.md
│   │       └── Core/README.md
│   └── harmonyos/README.md
├── contracts/README.md
├── docs/
│   ├── architecture/
│   ├── decisions/
│   ├── workflows/
│   ├── specs/README.md
│   └── superpowers/specs/
└── tooling/
    ├── scripts/
    └── templates/
```

`docs/specs/` is for future product behavior. `docs/superpowers/specs/` preserves this template-design artifact; it does not require downstream users to install a particular agent workflow.

Reserved feature, core, and build-logic directories contain useful boundary and extension guidance. They are not empty compiled modules or packages. The initial Android build has one application module; the initial iOS project has one application target. Add real libraries when the first responsibility needs them, and introduce Gradle convention plugins when multiple modules share build configuration.

## Native architecture

### Responsibilities and dependency direction

The application entry point owns startup, root navigation, and dependency assembly. Future feature modules own cohesive user-facing behavior, including presentation, state, and feature-specific logic. Core modules expose deliberately shared capabilities with small public interfaces.

Compile-time dependencies flow from app to features and core, and from features to core. Core must not depend on features or the app. Features must not import another feature's implementation. Cross-feature navigation is coordinated by the app; genuinely shared contracts can be extracted into a narrowly scoped core module.

Use Gradle dependencies and Kotlin visibility to express Android boundaries. Use local Swift packages, target dependencies, and Swift access control to express iOS boundaries. Once modules exist, their manifests must make dependency direction reviewable. This initial slice does not build a custom dependency-analysis tool for nonexistent modules.

### State and data flow

Future screens render explicit state and send user actions to a feature-owned state holder. Android can use ViewModels and observable state; iOS can use SwiftUI observation and actor isolation appropriate to the feature. Both follow the same behavioral requirements without requiring identical class structures.

Repositories or service interfaces separate presentation from external systems when external systems are introduced. Add a domain/use-case layer when business rules justify it. An empty application needs neither repositories nor a dependency-injection framework.

Failures from future external services must become explicit feature state with a documented recovery path. There is no network or persistence layer to implement in this slice.

### Shared material

`contracts/` will hold language-neutral interface schemas, fixtures, and shared behavioral examples when a product exists. It is not a shared runtime library. Platform-local models remain local unless a concrete external contract requires otherwise.

## Empty applications

Each app launches into one neutral native screen containing the temporary app name. This is build and launch scaffolding, with no sample product, navigation flow, settings feature, network access, persistence, authentication, analytics, or extra permissions.

Android uses Kotlin, Compose, a manifest with a launcher activity, and standard application resources. iOS uses a SwiftUI app entry point, application resources, and a checked-in shared scheme supporting command-line builds. Both retain normal native IDE workflows.

The Android Gradle wrapper, including its JAR and scripts, is checked in with a pinned distribution and SHA-256 checksum. Dependency versions are centralized in a version catalog. Implementation selects a compatible stable AGP, Gradle, Kotlin/Compose compiler, Compose BOM, and compile SDK set using official compatibility guidance, pins the exact versions, and records the versions that actually passed verification. It must not use dynamic versions.

iOS uses the selected Xcode toolchain and records the Xcode version used for verification. It has no external Swift package dependencies initially. Simulator builds do not require a development team or distribution credentials.

## Commands and build isolation

Expose these commands from the repository root:

```sh
./tooling/scripts/doctor android
./tooling/scripts/doctor ios
./tooling/scripts/doctor all
./tooling/scripts/build android
./tooling/scripts/build ios
./tooling/scripts/build all
./tooling/scripts/verify android
./tooling/scripts/verify ios
./tooling/scripts/verify all
```

Scripts resolve the repository from their own location and work when invoked by absolute path from another working directory. Validate arguments before starting native tools. A platform command checks only that platform's prerequisites. `all` requires both platforms and reports each result; it must never silently skip a missing platform and report complete success.

- Android builds invoke the wrapper under `apps/android/` and produce a debug APK in Android's own build directory. Verification also runs Android lint.
- iOS builds invoke the committed project and shared scheme for a generic iOS Simulator destination, with derived data under an ignored `.build/ios/` directory. Verification confirms the expected simulator application artifact. Compilation is not reported as a simulator launch test.
- `build all` and `verify all` may run their platform steps sequentially for readable output. Independence is also checked by explicitly running the individual build commands concurrently.
- Each command preserves the native command's failure status, identifies the failed platform, and provides a log location. Platform logs use separate directories under ignored `.build/` output.
- A build must not write into the other platform's project, alter tracked source/configuration, or change system-wide SDK selections and shell configuration. Standard native tool caches may be used; cache reuse does not introduce a cross-platform project dependency.
- User-supplied SDK and toolchain paths are honored and validated. Android documentation explains `JAVA_HOME` and `ANDROID_HOME`; machine-specific paths are never committed. iOS supports command-scoped `DEVELOPER_DIR` selection without changing the global Xcode selection.
- Project caches, build outputs, logs, IDE user settings, `local.properties`, and signing material are ignored by version control.

`doctor` reports actionable missing prerequisites without automatically installing SDKs or rewriting the user's environment. Native build commands remain documented in each platform README so the shared wrappers are convenient entry points rather than mandatory build infrastructure.

## Human and agent workflow

The root `AGENTS.md` is a short map of the repository, shared architecture rules, commands, and evidence requirements. Platform instructions add language and toolchain details. Contributor documentation covers the same conventions for humans; rules are defined once and referenced rather than duplicated across agent products.

Provide lightweight templates for a feature specification, a work plan/handoff, and an architecture decision. A work item identifies intended behavior, affected platforms, permitted scope, acceptance criteria, verification commands, and its completion boundary. It can cover one platform or several, and records platform-specific behavior deliberately.

The normal loop is to read the relevant guidance, inspect the affected code, implement within the assigned scope, verify, and report the result with file references and command evidence. Unrun checks are marked unverified, and missing toolchains are recorded as environment blockers. A task is not complete merely because its instructions or directory structure exist.

Assisted and autonomous execution use the same files and commands. Tasks define when human input is needed and what outcome is authorized; routine edits and checks within that scope do not acquire extra approval steps from this template. A downstream agent runner remains responsible for scheduling and invoking agents. The repository supplies discoverable instructions and verifiable work boundaries.

Concurrent contributors work in separate checkouts/worktrees or with explicitly disjoint ownership. Same-checkout build concurrency is supported across Android and iOS; concurrent edits to shared instructions or build manifests still require coordination.

## Verification and acceptance criteria

The first implementation is accepted only with evidence for all required checks below:

1. Open/import Android as an independent Gradle project and build its debug APK using the documented wrapper command. Xcode is not consulted by the Android scripts.
2. Build iOS for a generic simulator destination using the committed shared scheme. Android tools are not consulted by the iOS scripts.
3. Run Android lint successfully. Do not add trivial application unit tests merely to produce a green test count.
4. From a clean project-output state, build Android followed by iOS. Rebuild Android after iOS to detect interference.
5. Run both platform build entry points concurrently and confirm both succeed with distinct artifacts and logs. The supported concurrency case is one build per platform, not two builds of the same target sharing an output directory.
6. Verify the commands work from outside the repository working directory, and that missing/invalid arguments fail clearly without starting a build.
7. Exercise prerequisite failures for each platform using controlled environment/tool overrides. A missing Android prerequisite must not prevent the iOS-only command from dispatching, and vice versa. Aggregate commands must report failure when any required platform fails.
8. Compare tracked files before and after verification. Native builds must leave source and build configuration unchanged, and expected output must be ignored.
9. Record exact toolchain versions, commands, exit statuses, artifact paths, and any environmental limitations in a verification note. Preserve a distinction between build, static analysis, and launch evidence.

Launch each app on an available emulator/simulator when the environment permits, confirming that the neutral screen appears. Launch results are reported separately and are not prerequisites for the central build-isolation claim. Device signing, release archives, store distribution, and future SDK compatibility are outside that claim.

Meaningful tests for command dispatch and failure propagation are appropriate because these scripts determine what agents can claim was checked. Introduce feature and behavior tests when features exist.

## Local environment observations

Read-only checks on 2026-09-20 found macOS on Apple Silicon, Xcode 27.0 with iOS and simulator SDKs, an Android SDK, and Homebrew JDK 17. The default `/usr/bin/java` invocation does not resolve a configured Java runtime. Android Studio also bundles a JDK, but the project must select a compatible JDK explicitly rather than rely on the IDE's runtime version.

The restricted session could not communicate with CoreSimulator services. This is an observed environment limitation, not evidence that either application fails to build or launch. No application build or runtime test has been performed at the design stage.

## Deferred work

HarmonyOS remains a documented future platform boundary and is excluded from `all` until an actual native project, prerequisites, and verification contract are added. Product features, network/storage implementations, shared design assets, release environments, signing automation, hosted CI, and agent-runner integrations are separate additions to this foundation.

## Source guidance

- [Android modularization patterns](https://developer.android.com/topic/modularization/patterns)
- [Apple: organizing code with local packages](https://developer.apple.com/documentation/xcode/organizing-your-code-with-local-packages)
- [AGP and Gradle compatibility](https://developer.android.com/build/releases/about-agp)
- [Android built-in Kotlin support](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Compose compiler and dependencies](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)

The structure and scope in this document are design decisions for this template. Source guidance informs the native mechanisms; it does not prescribe this entire repository layout.

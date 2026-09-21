# HarmonyOS native foundation

Status: proposed for written review, 2026-09-21. The foundation scope and design direction were approved in conversation; implementation has not started.

## Purpose

Give developers a buildable Huawei HarmonyOS phone/tablet application alongside the independent Android and iOS projects. A developer can open the native project in DevEco Studio and use the repository's familiar doctor, build, and verify commands.

The selected first milestone is a native app and tooling foundation. Environment and design-library parity will follow as separate work. This specification does not add product-domain behavior or OpenHarmony distribution support.

## User-visible behavior

The application opens a single native ArkUI screen displaying its localized application name, `NativeTemplate`. Text follows platform appearance and text scaling and remains readable when the phone or tablet window changes size. The screen has no interactive controls, navigation, environment selector, or network request.

Use the bundle identifier `com.example.nativetemplate`, one default build product, and Debug and Release build modes. Both modes use the same identity. Public naming and resources belong to the HarmonyOS project and can be renamed through documented native configuration changes.

The initial compatibility floor and target are HarmonyOS 6.1.1 / API 24. This is a deliberate first-milestone boundary matching the installed SDK. Supporting older HarmonyOS versions requires a later compatibility decision and runtime evidence; installing an SDK does not establish device compatibility.

## Platform coverage

- Huawei HarmonyOS phones and tablets gain a native project under `apps/harmonyos/`.
- Android and iOS retain their native inputs and behavior. Their platform commands must work without HarmonyOS tooling installed.
- Shared tooling gains the `harmonyos` argument and aggregate dispatch after the new platform's native verification succeeds.
- The initial supported command host is macOS on Apple silicon, matching the inspected installation. Other host configurations require their own validation before support is claimed.

Deferred work includes dev/staging/production configuration, the health-request example, reusable design components, feature libraries, backend integration, store publication, release signing automation, and CI-provider setup.

## Native architecture

Use a standard DevEco Stage-model ArkTS/ArkUI project with one application module named `entry`. App startup loads the landing page; the page reads localized resources. There is no service, repository, persistence layer, or asynchronous product state. Startup failures must remain diagnosable through native logging.

Expected ownership is:

| Location | Responsibility |
| --- | --- |
| `apps/harmonyos/AppScope/` | Application identity and application resources |
| `apps/harmonyos/entry/` | Ability lifecycle, landing page, module manifest, and resources |
| `apps/harmonyos/build-profile.json5` and module build profile | Native products, build modes, SDK selection, and module registration |
| `apps/harmonyos/hvigorfile.ts`, module Hvigor file, and `hvigor/` | Native build configuration |
| Native OHPM manifests and lockfiles | Reproducible application dependencies |
| `apps/harmonyos/code-linter.json5` | Explicit native lint configuration |
| `apps/harmonyos/README.md` and `AGENTS.md` | Setup, commands, ownership, and verification guidance |

The native IDE and command-line tools consume the same checked-in project. A native build must not require a repository-root generator or either other platform. Keep generated output, dependency caches, local SDK configuration, signing material, and IDE user state ignored.

Add core or feature libraries only for concrete responsibilities. Future dependency direction remains app to features/core and features to core; core cannot depend on app or features. A feature cannot import another feature's implementation. See [decision 0003](../decisions/0003-harmonyos-native-foundation.md).

## Toolchain baseline and selection

The following installation metadata was inspected on 2026-09-21. These are observed versions, not successful native-build evidence:

| Tool | Observed version |
| --- | --- |
| DevEco Studio | 6.1.1.300 |
| Bundled HarmonyOS SDK | 6.1.1.125, Release, API 24 |
| Hvigor and HarmonyOS Hvigor plugin | 6.24.4 |
| OHPM | 6.1.2.285 |
| Bundled Node | 18.20.1; executable version check succeeded |
| Bundled emulator tooling | 6.1.1.350; runtime images and launch unverified |

Use the bundled toolchain as the initial baseline. Check in the matching native build configuration and resolved dependency locks. Document the native dependency-install command and let build/verify prepare required dependencies with the selected OHPM before compilation. A dependency-resolution failure is a command failure; installation must not silently rewrite tracked manifests or lockfiles.

`DEVECO_STUDIO_HOME`, when set, identifies the DevEco Studio application bundle on macOS. Otherwise discover the conventional system or user Applications installation. An explicit empty, missing, or unusable location fails with a diagnostic; it must not silently fall back to another installation. Ambiguous discovered installations require the override.

Derive the SDK, Node, Hvigor, OHPM, and lint tool locations from the selected installation. Scope environment changes to the command process. Do not change shell startup files, global package-manager configuration, or global SDK selection. Personal installation paths must not be committed.

Doctor reports observed versions and checks the SDK/API and tool compatibility required by the project. The initial documented support claim is limited to this baseline. Validate a native lint invocation against this installation during implementation; the presence of an IDE lint plugin alone is not proof of a working command-line lint check. If another official tool package is required, report that prerequisite explicitly and leave lint unverified until it runs.

## Shared command contract

Keep the existing one-platform-argument interface and extend `tooling/scripts/lib.sh` without a general dispatcher refactor.

| Command | Required behavior |
| --- | --- |
| `doctor harmonyos` | Check the selected installation, required SDK/compiler tools, Node, Hvigor, OHPM, lint tooling, and native project inputs; report versions and actionable failures |
| `build harmonyos` | Run doctor, resolve dependencies, build the default Debug product without personal signing credentials, and validate the resulting unsigned HAP |
| `verify harmonyos` | Run doctor, resolve dependencies, build and validate Debug and Release unsigned HAPs, and execute the checked-in native lint configuration |

Doctor does not install tools. Build and verification require no running emulator, attached device, Huawei account, or signing certificate. Device installation and signing are documented separately using native tools and local configuration.

Validate each produced package's readable structure and metadata against the checked-in bundle identifier, `entry` module name, and phone/tablet device declarations. Check a native debuggability/build-mode field when the package exposes one; otherwise record that package-level mode validation is unavailable and retain the exact native build invocation as mode evidence. Artifact existence alone is insufficient. A native tool failure remains a failure even if an old package is present.

Publish validated command artifacts to `.build/harmonyos/artifacts/debug/NativeTemplate.hap` and `.build/harmonyos/artifacts/release/NativeTemplate.hap`. Clear the destination for the mode being attempted before starting its native build, then copy a package only after that build and its metadata validation succeed. Debug and Release native builds execute sequentially and are inspected immediately, so one mode cannot accidentally validate the other's output. Native commands used directly may retain their standard project-local outputs.

Logs use `.build/logs/harmonyos/<command>.log`, following the existing replace-per-command behavior. Commands work from another working directory and with spaces in repository or toolchain paths. One build per platform may run concurrently; simultaneous HarmonyOS builds require separate checkouts/output directories.

Preserve existing statuses: usage errors return 2, prerequisite/artifact errors return 1, and native-process failures retain their nonzero exit status. Lint findings configured as errors must produce a failing command even if the selected native runner requires inspecting a report to detect them. Warnings remain visible in the log.

After genuine HarmonyOS verification establishes the new platform, change `all` to attempt `android`, `ios`, and `harmonyos` in that order and return the first failure. This is a checked-in dispatch change made with supporting evidence, not a runtime auto-detection feature. Missing SDKs fail their platform; aggregate commands continue to attempt the others. Developers needing only one platform can continue selecting it explicitly.

## Shared contracts and documentation

The existing environment and design-library contracts continue to cover Android and iOS. Document HarmonyOS as foundation-only until later parity work explicitly extends those contracts. Do not add empty entries to environment parity tests or manufacture passing component coverage.

Implementation updates the root README, architecture/toolchain guidance, development and verification guides, HarmonyOS platform guidance, ignore rules, and shared command tests. Record exact commands, native versions, artifact paths, and limitations in a dated verification record under `docs/workflows/`.

The platform README explains IDE import, toolchain selection, dependency installation, direct native build/lint commands, unsigned artifacts, local signing and launch, application renaming, and future module boundaries.

## Acceptance scenarios

1. With the documented toolchain installed, a developer opens `apps/harmonyos/` in DevEco Studio and the project imports using its native manifests.
2. With the documented tools and resolved dependencies, `build harmonyos` creates a validated unsigned Debug package without signing credentials or another platform's SDK.
3. `verify harmonyos` succeeds only when both native build modes, both artifact validations, and native lint succeed. The report identifies exactly which checks executed.
4. Missing tools, an invalid explicit installation, dependency-resolution errors, compilation errors, lint errors, absent output, or mismatched metadata cause a nonzero result with an actionable log. An old output cannot turn a failed native command into success.
5. The default app can be installed and launched using documented native emulator/device steps once the required runtime and local signing configuration are available. Its landing text is readable in phone and tablet configurations. Record each actual launch configuration separately.
6. Android and iOS commands run without HarmonyOS prerequisites. Aggregate commands attempt all three platforms and retain the first failure. Concurrent builds of different platforms do not overwrite each other's output or logs.
7. Builds and IDE import leave tracked native inputs unchanged; generated files, machine configuration, and credentials remain ignored.

## Verification and completion boundary

Required implementation evidence:

```sh
python3 -m unittest discover -s tooling/tests -v
./tooling/scripts/doctor harmonyos
./tooling/scripts/build harmonyos
./tooling/scripts/verify harmonyos
./tooling/scripts/doctor android
./tooling/scripts/verify android
./tooling/scripts/doctor ios
./tooling/scripts/verify ios
./tooling/scripts/verify all
```

Extend the real-shell/fake-tool tests for prerequisite isolation, installation selection, path handling, Debug/Release dispatch, dependency/lint failures, missing/mismatched package metadata, stale artifacts after a failed build, aggregate results, and concurrent platform outputs. Keep these tests distinct from native compilation and lint evidence. Check tracked-file status before and after native verification, and perform clean/sequential/concurrent isolation checks following [the verification workflow](../workflows/verification.md#isolation-checks-for-build-changes).

Record DevEco import and emulator/device smoke checks separately from command verification. The smoke check proves startup and visible content; there is no product logic or reusable component in this milestone requiring a fabricated unit-test suite. Add behavior tests when subsequent work introduces such behavior.

The buildable-foundation claim requires successful real HarmonyOS builds, lint, artifact checks, shared tooling tests, and DevEco project import. Run Android and iOS verification when their toolchains are available, as required by the repository. Missing tools or unrun checks remain explicitly unverified and limit the evidence claim. Emulator/device launch is attempted when the runtime is available; unavailable images, devices, or signing access are documented limitations and do not become an implied runtime pass. Store readiness and older-OS support are outside this completion boundary.

This design-writing change contains documentation only. Native builds, lint, IDE import, and launch have not been performed for HarmonyOS at this stage.

## References

- [Independent native projects](../decisions/0001-independent-native-projects.md)
- [Repository architecture](../architecture/overview.md)
- [Huawei command-line tools](https://developer.huawei.com/consumer/cn/doc/harmonyos-guides-V5/ide-commandline-get-V5): tool roles; installed metadata above determines this proposal's version baseline.
- [Huawei native build modes](https://developer.huawei.com/consumer/cn/doc/HarmonyOS-Guides/ide-hvigor-compilation-options-customizing-sample)
- [Huawei testing overview](https://developer.huawei.com/consumer/cn/testing/get-started/)

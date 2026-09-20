# Native environment verification

## Objective and scope

Implement the [approved environment design](../specs/2026-09-20-native-environments.md): native dev/stg/prod configuration, separate app identities, independent Debug/Release modes, and a dummy health-request example. The completion boundary is a tested local feature branch; publishing, signing for distribution, and store submission are outside this work.

Changed areas: Android application/configuration/tests; iOS application/configuration/project/schemes/tests; shared verification scripts and tests; current guides and [decision 0002](../decisions/0002-native-environment-configuration.md). The design-system implementations are unchanged.

## Environment

Android: JBR 25.0.3, Gradle 9.4.1, AGP 9.2.1, compile/target SDK 36 and build tools 36.0.0. Runtime checks used a dedicated Pixel 7 emulator with the installed API 36 Google APIs ARM64 16 KB image.

iOS: Xcode 27.0 (27A266a), simulator SDK/runtime 27.0, Swift 6 language mode, and a dedicated iPhone 18 Pro simulator. Existing personal IDE/device sessions were not controlled.

## Commands and measured results

| Check | Result | Evidence |
| --- | --- | --- |
| Both platform doctor commands | Passed, exit 0 | Required SDK/JDK/Xcode, aapt2, and plutil available |
| Shared Python suite | 35 tests passed | Real shell dispatch with fake native tools; parity and scheme contracts |
| Android native variant matrix | All 6 built and linted | dev/stg/prod × Debug/Release |
| Android app tests | 30 passed | 5 tests for each of 6 compiled configurations |
| Android design-system tests | 9 passed | Existing Robolectric component suite |
| Invalid Android native configuration | Rejected as expected | Missing API key and HTTP URL fail with file/key diagnostics in a disposable copy |
| iOS native configuration matrix | All 6 built | Debug configurations also compile both test targets; Release builds exclude the Debug fixture |
| iOS resolved configuration checks | Passed | Identifier, display name, canonical environment, full URL, and DEBUG condition verified for every configuration |
| iOS app tests | 21 executions passed | 7 XCTest cases × 3 schemes, executed on the dedicated simulator |
| iOS component interactions | 24 executions passed | Existing 8 UI tests × 3 schemes, executed separately from compilation |
| Updated root verify commands | Both passed, exit 0 | Actual native matrices and per-variant artifact validation |
| Android installation/launch | Passed for all 3 identities | Coexistence, launch, visible endpoint, and request text checked |
| iOS installation/launch | Passed for all 3 identities | Coexistence confirmed with simctl; screenshots inspected |
| Configuration-only endpoint edits | Passed on both platforms | Disposable copies changed only dev endpoint to a /v2/ base path; app tests and displayed /v2/health request verified |
| Larger text | Inspected on both platforms | Android font scale 2.0 and iOS accessibility-large; endpoint and full request wrap without ellipsis |
| Build isolation | Passed | Native clean; Android → iOS → Android; concurrent builds from an outside working directory; 109 tracked inputs and Git status unchanged |
| Generated-output isolation | Passed | Build outputs and evidence remain ignored |

Primary repeatable commands:

```sh
./tooling/scripts/doctor android
./tooling/scripts/doctor ios
python3 -m unittest discover -s tooling/tests -v
./tooling/scripts/verify android
./tooling/scripts/verify ios
```

The native matrix invokes assemble/lint/unit-test tasks for DevDebug, DevRelease, StgDebug, StgRelease, ProdDebug, and ProdRelease. It also runs the existing library lint/test tasks. Release APKs remain unsigned.

The iOS matrix uses NativeTemplate-Dev/Stg/Prod with matching Debug/Release configurations. AppTests and DesignSystemUITests were executed on a dedicated simulator with explicit destinations, separate DerivedData paths, parallel testing disabled, and ad-hoc simulator signing. See the [iOS test command](../../apps/ios/README.md#execute-tests); generic-simulator build-for-testing is not counted as executed XCTest.

The endpoint-edit probes also passed 5 Android app tests and 7 iOS app tests in their disposable copies. Their source configuration changes were not applied to the feature branch.

## Behavior observed

Each installed app displayed its correct name, environment, and HTTPS endpoint. The example built a GET request ending in /health. Changing only the endpoint file in a disposable native project to a /v2/ base path changed the displayed request to /v2/health. The example sends no request and creates no server response.

Configuration parsing and request construction were tested before implementation. Initial tests failed on missing types/adapters; the iOS packaged-configuration test failed on a missing AppEnvironment plist entry before scheme/plist wiring. The shared command tests exposed the old single-Debug dispatch before the matrix implementation.

## Local artifacts

Generated evidence is ignored and local to the feature worktree; it is not a portable Git artifact:

- `.build/environment-evidence/`: environment screenshots, Android UI hierarchy snapshots, archived logs, test-result XML, and isolation summary.
- `.build/ios-environment-tests/{dev,stg,prod}-ui-results.xcresult`: executed component UI-test results.
- `.build/ios-environment-tests/{dev,stg,prod}/Logs/Test/`: executed app-test results.
- `.build/ios-config-probe/Logs/Test/`: the iOS endpoint-edit probe.
- `.build/logs/{android,ios}/{doctor,verify,build}.log`: shared-command output; later runs replace these files.

The final isolation checks clean native outputs and rebuild the default dev Debug apps. The archived evidence records the earlier complete matrices; it does not imply every variant's generated output remains after cleaning.

## Implementation decisions and limits

AGP required explicit resValues enablement and explicit Release host-test registration. The matrix verifies both settings; without them the relevant resources/tasks would not build. Kotlin AppConfig uses a plain immutable class so a generated data-class copy cannot bypass validation; no existing consumer requires copy or structural equality.

Native command-line builds, metadata inspection, simulator/emulator launch, and the listed tests are verified. IDE import/build was not exercised in an interactive IDE session. Physical-device signing, release archives, store distribution, minimum-OS runtime behavior, and screen-reader interaction remain unverified.

Robolectric emitted existing JDK native-access warnings; the component suite passed. No native dependency/toolchain upgrade was made to suppress those warnings.

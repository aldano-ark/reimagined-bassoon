# Native design-library verification — 2026-09-20

## Scope and environment

Verified the Android `:core:designsystem` library and the iOS local `DesignSystem` package, their app integrations, debug previews, component behavior, and shared verification dispatch.

Host: macOS 27.0, Apple Silicon. Android: JBR 25.0.3, Gradle 9.4.1, AGP 9.2.1, Kotlin/Compose compiler 2.2.10, existing Compose BOM 2026.02.01, SDK/build tools 36/36.0.0. Component tests use Robolectric 4.16.1, JUnit 4.13.2, and Android API 35 test runtime. iOS: Xcode 27.0 (27A266a), Swift 6.4 in Swift 6 language mode, simulator SDK/runtime 27.0; deployment minimum remains iOS 17.

The existing Java 25 daemon criteria, native app identifiers, minimum OS versions, and Java 17 bytecode target were preserved.

## Results

| Check | Result |
| --- | --- |
| Android component behavior | 9 tests passed, zero failures/errors |
| Android app debug build and lint | Passed |
| Android library debug lint | Passed, zero errors; one existing-SDK-version notice |
| Android library Release build | Passed; debug preview code is not part of the Release source set |
| SwiftUI component interactions | 8 XCTest tests passed, zero failures; native runner exit 0 |
| iOS Debug/test-target compilation | Passed through `build-for-testing` |
| iOS Release app/package compilation | Passed with debug preview/test-host code excluded |
| Shell command behavior | 20 tests passed |
| Expanded `verify all` | Passed; Android executes library tests/lint, iOS compiles its UI-test target |
| Concurrent Android and iOS builds | Both exited 0 when invoked from outside the checkout |
| Source/configuration integrity | 77 project files retained identical SHA-256 hashes across integration/concurrent builds |
| Disposable simulator cleanup | Test-created devices removed; existing devices were not selected |

## Android behavior covered

- All three button intents activate their caller callback.
- Disabled buttons do not activate for any intent.
- Loading retains its accessible name/state and blocks activation.
- The caller can end loading and re-enable an action.
- Field input reaches caller-owned state, and external state replacement updates the field.
- Error text supersedes supporting text and is exposed in editable-field semantics.
- Disabled field state is exposed.
- Status title/description/action are accessible and the action invokes its caller callback.
- A long label fits inside its button at enlarged text size in RTL layout, with at least the native 48 dp target.

All nine tests first failed against nonfunctional API stubs, then passed against the implemented components. They execute actual Compose semantics/interactions under Robolectric, not a mock component implementation.

## SwiftUI behavior covered

Executed `DesignSystemUITests` on a separately created iPhone 18 Pro simulator running iOS 27.0:

- `testLoadingButtonKeepsNameAndCannotActivate`
- `testDisabledAndInheritedDisabledButtonsCannotActivate`
- `testSecondaryAndDestructiveButtonsActivate`
- `testFieldUsesCallerStateAndExposesError`
- `testFieldAcceptsTapsAcrossMinimumTouchTarget`
- `testDisabledFieldsDoNotFocusFromExpandedTarget`
- `testStatusActionIsOwnedByTheCaller`
- `testLargeTextRtlActionRemainsUsable`

The suite verifies real controls in a debug-only host selected by launch arguments. The field test checks both the editable element and visible caller state after input, then an externally supplied replacement and accessible error state. Ordinary app launches still display the neutral screen.

The original six tests first failed against nonfunctional stubs; a focused field assertion also demonstrated that a constant binding did not update caller state. An additional physical edge-tap regression exposed that a 44-point layout frame alone did not focus the smaller native field. The corrected field uses an explicit interaction shape and simultaneous tap forwarding to SwiftUI focus, guarded by enabled state. A disabled-field check protects against activating either explicitly or parent-disabled fields.

The edge test waits for the native keyboard before typing; an initial immediate-typing attempt raced the first keyboard presentation. The final suite executed all eight tests with zero failures in about 72 seconds, and `xcodebuild` returned 0. Release compilation also passed after the interaction fix.

Final local result bundle:

```text
.build/ios-design-results/46F37754-CD97-4BA2-B84D-2CD9B6B1B343.xcresult
```

This is an ignored local artifact, not a portable Git file. New executions produce new result bundles. Computer-use tools were not invoked; the user's existing IDE/simulator session was not controlled.

## Verification commands

```sh
./tooling/scripts/verify all
python3 -m unittest discover -s tooling/tests -v
```

Android verification includes `:app:assembleDebug`, `:app:lintDebug`, `:core:designsystem:lintDebug`, and `:core:designsystem:testDebugUnitTest`. iOS verification compiles tests; the separate XCTest invocation in the [iOS library guide](../../apps/ios/Packages/Core/DesignSystem/README.md) executes them.

Four new shell expectations failed before dispatch changes, then all 20 passed. They ensure library lint/tests and iOS test compilation are requested, and failures propagate even when an application artifact already exists.

## Previews, limits, and execution decisions

Both native preview galleries compile and define light/dark, larger text, RTL, and component-state variants. They have not been manually inspected in the IDE during this task. Automated interaction/layout checks do not constitute a full visual, VoiceOver, or TalkBack audit. No minimum-OS device run, Android instrumented-device run, alternate host run, or HarmonyOS implementation is claimed.

Existing Android app lint upgrade/backup notices remain; the design library has one compile-SDK upgrade notice. Robolectric emits a JDK native-access warning while its tests pass. Xcode emits App Intents metadata notices for targets that do not use that framework. No toolchain upgrade was needed.

During intentional failing XCTest runs, automatic `simctl diagnose` collection took several minutes after assertions finished. Only those test-owned diagnostic child processes were stopped; subsequent component runs used `-collect-test-diagnostics never`, retaining XCTest assertions and result bundles while omitting verbose simulator diagnostic archives. An early scratch-runner edit also interrupted post-run shell reporting; later runs used immutable per-run script copies and completed normally. These scratch helpers are not shipped build tooling.

## Independent review

The independent review found no confirmed Critical or Important issues and no Minor findings worth delaying integration. It reran all 20 command tests successfully and confirmed a clean checkout/whitespace check. Native results were reviewed from the recorded evidence rather than rerun by the reviewer.

The following limits remain explicit:

- The field focus correction is covered for edge activation, disabled state, binding, and error behavior. Selection gestures, marked-text input methods, and caller-applied programmatic focus have not each received a separate native regression check. The native field still performs editing; consumers depending on additional focus interactions should verify those flows.
- Button minimum height and field-edge targets were exercised. Horizontal touch bounds for extremely short iOS button labels were not separately measured; no width defect was demonstrated.
- Manual preview appearance, VoiceOver/TalkBack, minimum-OS runs, and physical-device behavior remain unverified beyond the recorded simulator/Robolectric checks.
- Password fields, masks, navigation, branding, and a larger component catalog remain intentionally outside this version.

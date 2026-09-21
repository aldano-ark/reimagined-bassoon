# Native design libraries

Use the shared component purposes below with native implementations on each platform. Android follows Compose Material 3; iOS follows SwiftUI styling, tint, Dynamic Type, and system appearance. The libraries do not impose a matching custom brand.

## Locate and consume

| Platform | Build boundary | Import and guide |
| --- | --- | --- |
| Android | Gradle `:core:designsystem` | `com.example.nativetemplate.designsystem.theme` and `.components`; [Android guide](../../apps/android/core/designsystem/README.md) |
| iOS | Local Swift package product `DesignSystem` | `import DesignSystem`; [iOS guide](../../apps/ios/Packages/Core/DesignSystem/README.md) |

Apps and features may depend on these core libraries. The libraries cannot depend on app startup, features, services, or the other platform.

## Component behavior

| Purpose | Android | iOS | State contract |
| --- | --- | --- | --- |
| Action | `DSButton` and `DSButtonIntent` | `DSButton` and `DSButtonIntent` | Primary/secondary/destructive; enabled and loading are caller-owned |
| Text input | `DSTextField(value, onValueChange, label)` | `DSTextField(label, text: Binding)` | Editing updates caller state; error replaces supporting text |
| Status message | `DSStatusView` and `DSStatusKind` | `DSStatusView<Action>` and `DSStatusKind` | Neutral/error message; optional caller-composed action |
| App theme | `DSTheme` plus `MaterialTheme` | Native SwiftUI environment/styles | Follow platform appearance and text scaling |
| Content spacing | `DSSpacing` in dp | `DSSpacing` in points | `xs`, `sm`, `md`, `lg`, `xl`, `xxl`: 4, 8, 12, 16, 24, 32 |

### Buttons

Loading displays a native progress indicator, preserves the action label, exposes a localized loading state, and disables activation. Disabling also prevents activation. The callback is a synchronous event; callers own asynchronous execution, failures, cancellation, and returning to an enabled state. Work already started is not canceled or deduplicated by the component.

iOS respects inherited disabled state. Android callers pass enabled state explicitly. Destructive intent uses semantic Material error colors on Android and the native destructive role on iOS. Native shape, font, feedback, and touch behavior remain intact.

### Fields

Callers own the value/binding and validation. Fields preserve typed input and forward native keyboard/IME or content/submit options. Supplying an error replaces ordinary supporting text. No password fields, input masks, or form framework are included.

Android exposes Material field/error semantics. iOS presents one accessible editable field label followed by the message, with the duplicate visible label hidden from accessibility. It does not repeat the same message as both text and a hint.

The iOS field retains native rounded styling while its surrounding 44-point interaction area forwards taps to SwiftUI focus. The same area stays inactive for explicitly or parent-disabled fields.

### Status views

Titles are required and act as accessible headings. Optional descriptions and actions retain reading order and logical start alignment. Caller-supplied error text communicates the problem without relying on color. Status views own no retry, networking, or navigation policy.

## Native appearance and accessibility

Android uses dynamic color on Android 12+ with Material 3 light/dark fallbacks; previews/tests can disable dynamic color and select appearance explicitly. iOS inherits semantic colors, text styles, and tint without a synthetic global theme.

Native controls retain their own internal metrics. Use content spacing around them, not to shrink touch targets. Controls support natural text wrapping and logical start/end layout; Android retains at least the native 48 dp target and iOS wrappers provide a minimum 44-point interactive height. Do not add fixed text heights to feature wrappers.

Supplied labels and descriptions are localized by the consuming app. Internal loading strings are resources: Android `src/main/res/values/strings.xml`, iOS `Resources/en.lproj/Localizable.strings`.

## Previews

- [Android gallery](../../apps/android/core/designsystem/src/debug/java/com/example/nativetemplate/designsystem/previews/DesignSystemPreviews.kt): light, dark, enlarged text, and RTL, with normal/disabled/loading/error examples.
- [SwiftUI gallery](../../apps/ios/Packages/Core/DesignSystem/Sources/DesignSystem/Previews/DesignSystemPreviews.swift): matching state coverage using native environment overrides.

Previews are debug-only. The iOS test host is also debug-only and selected solely by test launch arguments. Ordinary app launches show the environment configuration and request example, using native text and the existing theme/spacing.

## Verification

```sh
./tooling/scripts/verify android
./tooling/scripts/verify ios
./tooling/scripts/test ios
python3 -m unittest discover -s tooling/tests -v
```

Android verification builds, lints, and tests all six app variants and executes the existing library checks once. iOS verification compiles app/unit/UI tests for all three Debug configurations and builds all three Release configurations. `test ios` executes app and component tests on an automatically managed dedicated simulator; use `test ios all` for all environments. The [iOS library guide](../../apps/ios/Packages/Core/DesignSystem/README.md) also documents component-only native execution.

Native UI tests and preview compilation have limited scope: record actual execution results separately from visual inspection, screen-reader inspection, and minimum-OS/device testing. See the [measured design-library verification](../workflows/design-libraries-verification-2026-09-20.md).

## Extending the libraries

Use native controls for behavior these components do not cover. Add a wrapper when it owns a concrete reusable policy or composition, and document the behavior it adds. Avoid aliases for every native token and control. Keep dependencies narrow, callers in charge of product state, and behavior tests/examples alongside the component that owns them.

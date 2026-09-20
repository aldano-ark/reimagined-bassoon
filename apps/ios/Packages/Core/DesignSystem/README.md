# iOS design system

A local Swift package exposing native SwiftUI components. The package depends only on Apple platform frameworks and supports iOS 17 and later.

## Consume

Add the local package product `DesignSystem` to an iOS target, then `import DesignSystem`. The template's app already links it. No package/project generator or Android toolchain is needed.

Use native semantic fonts, foreground/background styles, and inherited tint. There is no parallel global theme object. `DSSpacing` provides `xs`, `sm`, `md`, `lg`, `xl`, and `xxl` content spacing values (4–32 points), while controls retain native internal metrics and shape.

## Components

| API | Purpose | Caller owns |
| --- | --- | --- |
| `DSButton` | Primary, secondary, or destructive native action; disabled/loading states | Action closure, enabled/loading state, asynchronous work |
| `DSTextField` | Visible label and controlled binding with supporting/error text | Value, validation, keyboard/content/submit configuration |
| `DSStatusView` | Neutral/empty or error message with optional action view | Copy and action/retry/navigation policy |

```swift
import DesignSystem
import SwiftUI

struct ExampleForm: View {
    @State private var name = ""
    let onContinue: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: DSSpacing.lg) {
            DSTextField("Name", text: $name, supportingText: "Enter your name")
            DSButton("Continue", action: onContinue)
        }
        .padding(DSSpacing.lg)
    }
}
```

Button titles and field/status labels accept `LocalizedStringKey`. Supporting and description strings are caller-provided copy; localize them in the consuming app. The library's loading accessibility string lives in `Sources/DesignSystem/Resources/en.lproj/Localizable.strings`.

`DSButton` preserves its label while loading, displays native progress, and disables activation. It respects a parent `.disabled(true)`. Primary actions use a native prominent bordered style; secondary/destructive actions use a bordered style, with the native destructive role. Callers own async execution and errors.

`DSTextField` forwards native keyboard type, content type, capitalization, autocorrection, submit label, and submit action. Error text replaces supporting text and remains an accessible element after the field. The separate visible label is hidden from accessibility because the field already exposes it. Input is not transformed or validated by the library.

The native rounded field keeps its visual metrics inside a minimum 44-point interaction area. Taps in that area request native SwiftUI focus using a simultaneous gesture, while explicit and inherited disabled state prevent activation. Native editing and selection remain on the underlying text field.

The tests cover edge focus, disabled state, and binding updates. Selection gestures, marked-text input methods, and caller-applied programmatic focus have not each been separately verified; check those interactions when a consuming feature depends on them.

`DSStatusView` marks its title as a heading and accepts optional `@ViewBuilder` action content. Use a native control directly when the library does not cover the required behavior.

## Previews

Open `Sources/DesignSystem/Previews/DesignSystemPreviews.swift` in Xcode. Debug-only galleries cover light/dark appearance, larger Dynamic Type, RTL, and normal/disabled/loading/error states. The library follows native environment values and does not force a product tint or custom font.

## Verification

All three environment schemes include `DesignSystemUITests`. From the repository root, `./tooling/scripts/verify ios` compiles the package, app, and test target. This is a compilation check, not a test-run result.

Run XCTest on an explicitly selected simulator:

```sh
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate-Dev -configuration Debug-Dev \
  -destination "platform=iOS Simulator,id=$NATIVE_DESIGN_TEST_DEVICE" \
  -derivedDataPath .build/ios-design-tests \
  -parallel-testing-enabled NO -only-testing:DesignSystemUITests \
  CODE_SIGN_IDENTITY=- test
```

Set `NATIVE_DESIGN_TEST_DEVICE` to a dedicated test simulator's UUID. Automated tests launch a debug-only fixture host using `--design-system-tests` and a scenario argument. Ordinary app launches show the environment configuration example; Release excludes the host and previews.

Tests exercise actions, disabled/loading behavior, parent-disabled state, text binding and error accessibility, physical taps across the minimum field target, status actions, and enlarged-text RTL interaction. They do not establish every visual layout or replace VoiceOver/device inspection.

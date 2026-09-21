# Native design libraries

Date: 2026-09-20

Status: Implemented and independently reviewed on 2026-09-20. Native component, build, release, and command checks pass. Manual visual/screen-reader checks and additional platform environments remain explicitly unverified.

## Intent and accepted direction

Prepare a reusable design library for each implemented native platform in the mobile monorepo. Android should feel native to Android, and iOS should feel native to iOS. The template still has no product domain or brand.

Provide predictable UI foundations and documented component behavior that humans and AI agents can discover and reuse. Preserve independent platform builds, existing deployment minimums, and the project-scoped Android Studio JDK configuration.

The user selected native appearance over a matching custom visual system. Shared concepts describe component purpose and behavior; each platform owns its native implementation, visual details, and APIs.

## Scope and alternatives

Use native themes and controls with a small reusable component layer. A tokens-only library would leave recurring behavior and usage conventions undefined. A comprehensive custom widget framework would add unnecessary abstractions and maintenance before a product exists.

This first version includes semantic styling guidance, spacing, three component families, native previews, usage documentation, meaningful component checks, and application integration. It does not introduce a branded palette, custom fonts, network dependencies, generated cross-platform tokens, a Figma library, or a runtime shared between Kotlin and Swift.

## Structure and dependencies

```text
apps/android/core/designsystem/
    build.gradle.kts
    README.md
    src/main/.../theme/
    src/main/.../components/
    src/debug/.../previews/
    src/test/...

apps/ios/Packages/Core/DesignSystem/
    Package.swift
    README.md
    Sources/DesignSystem/
        Foundations/
        Components/
        Previews/

apps/ios/DesignSystemUITests/

docs/design-system/
    README.md
```

Android registers `:core:designsystem` as an Android library with Compose enabled. iOS exposes a local Swift package product named `DesignSystem`. The existing application targets depend on their respective library. Neither library depends on an app, feature, other platform, or root script for compilation.

Use the existing version catalog and pinned Android toolchain. Add only the Android library plugin alias and native preview/test dependencies needed by the implementation. SwiftUI and platform frameworks supply iOS UI behavior; no third-party Swift UI framework is required. Build configuration must retain Android API 26 minimum, compile/target SDK 36, Java 17 bytecode, and iOS 17 minimum.

## Foundations

### Android

Expose `DSTheme` as the app's Compose theme entry point. It follows the system light/dark setting by default. Use Material 3 dynamic color on supported devices, with Material 3 light/dark defaults as the fallback. Allow callers to disable dynamic color and select a light/dark mode for deterministic previews and tests.

Typography, colors, and component shapes use Material 3's semantic roles. Consumers can use `MaterialTheme` directly inside `DSTheme`; the library does not create redundant aliases for every native token. Preserve native text scaling and use layout constraints that accommodate wrapping.

### iOS

Use SwiftUI semantic foreground styles, adaptive system background colors, text styles, and native button/text-field styling. Follow the environment's appearance, Dynamic Type, locale, layout direction, and control state. Do not hardcode Material colors, custom font sizes, or Android shapes into iOS controls.

Use standard SwiftUI modifiers and styles for app-specific customization. No synthetic global theme object is required merely to imitate Android's theme API. Respect inherited tint and let the OS render native styles appropriate to the supported version.

### Spacing and shape guidance

Provide `DSSpacing` values for small through large content spacing: 4, 8, 12, 16, 24, and 32 logical units, expressed as `Dp` on Android and `CGFloat` on iOS. Name the values consistently in documentation; their use remains semantic guidance rather than a requirement to override native control metrics.

Native controls retain their own minimum sizes, corner styles, typography, and internal padding. Do not force identical pixel geometry across platforms. Add custom shape tokens only when the supplied components need them; no unused radius/elevation scale is part of this version.

## Component contract

Public names use a short `DS` prefix, with platform-idiomatic parameter types and composition. Components receive state and callbacks; they do not perform networking, validation business rules, navigation, or asynchronous work themselves.

### DSButton

Support primary, secondary, and destructive intent, plus enabled and loading state. Android maps these intents to Material 3 button treatments and appropriate semantic colors; iOS uses native button styles and destructive role.

Loading disables interaction, preserves the action's accessible name, and presents a native progress indicator with an accessible loading state. A disabled or loading button does not invoke its action. SwiftUI's inherited disabled state is respected; Android callers pass enabled state explicitly. Labels can wrap without a fixed height that clips accessibility text.

The callback is a synchronous event notification. The caller owns starting work, updating state, handling failure, and ending loading. The component makes no claim of canceling or deduplicating work already started by the caller.

### DSTextField

Provide a visible label, controlled text value/binding, enabled state, and optional supporting or error text. The caller supplies error state; the component never invents validation rules or changes the user's text.

Use a Material 3 labeled field on Android. On iOS, compose a visible label, native SwiftUI text field, and supporting message with system typography. Error text supersedes ordinary supporting text and is exposed to accessibility. Preserve the native editing, selection, input-method, and focus behavior.

Expose native keyboard/content/submit configuration rather than a restrictive shared keyboard enum. Password fields, rich text, input masks, and a general form framework are outside this version.

### DSStatusView

Present a title, optional description, and an optional caller-supplied action for neutral/empty and error states. Use semantic text and error treatments appropriate to each platform. Communicate the state with text rather than color alone.

Titles are required parameters, and callers provide accessible, localized content. Preserve a readable content order for assistive technology. Use optional composed action content so callers can reuse `DSButton` or an appropriate native control. The status view owns no retry policy, networking, or navigation.

## Previews and app integration

Provide native component previews for normal, disabled, loading, supporting-text, and error states as applicable. Include light/dark previews, larger text, and right-to-left layout examples. Android previews disable dynamic color so their appearance is reproducible. SwiftUI previews use native environment overrides.

Keep Android previews in the debug source set and Swift previews under debug compilation. A preview gallery groups the components for discovery. It is an IDE preview composition, not a product feature or a new application navigation flow.

The Android app adopts `DSTheme` and imports the design library. The iOS app links the local package and uses its spacing/foundation API in the existing neutral screen. Both retain the `NativeTemplate` screen and remain independently buildable. Component examples belong in previews and READMEs.

## Accessibility and interaction requirements

- Preserve native button, editable-text, disabled, and destructive semantics.
- Loading state must retain the action label and prevent activation.
- Visible field labels and error descriptions must be accessible without duplicate announcements introduced by the wrapper.
- Allow system text scaling, natural wrapping, and right-to-left layout. Avoid fixed text-container heights and forced left/right alignment.
- Keep native touch-target behavior; Android controls use at least the native 48 dp target and iOS interactive components provide at least a 44-point target where the native style does not already supply it.
- The first version adds no custom animation or gesture system that would bypass reduced-motion or native interaction behavior.

## Human and agent guidance

`docs/design-system/README.md` maps component purposes to the two native APIs and states their ownership boundaries. Each library README includes import/dependency setup, public API examples, state ownership, theme behavior, preview entry points, and checks.

Update platform contributor guidance to direct feature authors to the design library for supplied patterns. Native controls remain appropriate for behavior the library does not cover. Do not wrap an entire native toolkit or introduce a second competing theme system in a feature.

When a component's behavior or public interface changes, update its examples and applicable checks. Shared names are not a promise that the Swift and Kotlin APIs or layouts are identical.

## Verification and acceptance criteria

1. Android builds the library and consumes it from `:app`; iOS resolves the local Swift package and consumes its product from the committed project. Neither platform needs the other's toolchain.
2. Both existing build/verification entry points continue to work. Android verification covers library lint as well as application lint; any command changes retain the existing failure/dispatch tests.
3. Meaningful component tests verify enabled versus disabled/loading activation, externally controlled field updates, and accessible field/error state where the native test harness supports them. Tests must exercise the component's behavior rather than assert literal spacing constants or native framework internals.
4. Compile preview and test targets for the supported platforms. Use native command-line test runners for applicable tests when the environment is available, and report compilation separately from test execution.
5. Preview definitions cover both appearances, enlarged text, and right-to-left layout. Claims of visual/accessibility runtime verification require observation or native test evidence; merely compiling a preview is not a visual pass.
6. Run the existing shell-command suite after tooling changes. Preserve nonzero failures and explicit unverified results if a simulator/emulator or test dependency is unavailable.
7. Recheck both native app builds and output isolation after integration. Verify that builds leave tracked source/configuration unchanged.
8. Document the actual public APIs, native differences, tested environments, command results, and any unverified runtime checks.

Computer control remains with the user. This work does not resume interactive IDE or simulator control. Native compilation and command-line verification remain available; visual preview inspection can be performed by the user and recorded separately.

Test-layout refinement: Android component behavior is exercised with Compose tests under Robolectric in the library's JVM test source set. SwiftUI interaction tests use an Xcode UI-test target and a debug-only app host instead of an empty Swift package test target. The host is selected only by test launch arguments; ordinary app launches retain the neutral screen. Run iOS tests on a separately created test simulator, leaving the user's existing simulator and IDE session alone.

## Deferred work

A custom brand, token generation, asset/icon/font distribution, broad component catalogs, navigation components, complex forms, animations, and screenshot-baseline infrastructure remain separate additions driven by concrete needs.

## Source guidance

- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose design systems](https://developer.android.com/develop/ui/compose/designsystems)
- [SwiftUI view styles](https://developer.apple.com/documentation/swiftui/view-styles)
- [SwiftUI ButtonStyle](https://developer.apple.com/documentation/swiftui/buttonstyle)
- [SwiftUI Font](https://developer.apple.com/documentation/swiftui/font)
- [SwiftUI Color](https://developer.apple.com/documentation/swiftui/color)

The module layout, public component set, spacing values, and behavior contracts above are proposed decisions for this template; platform documentation supplies the underlying native mechanisms.

# Android design system

Native Compose/Material 3 foundations and components for the template. This Android library depends on no app, feature, iOS project, or root script.

## Consume

Add `implementation(project(":core:designsystem"))` to a consumer's Gradle dependencies. Public APIs live under `com.example.nativetemplate.designsystem.theme` and `.components`.

Wrap UI in `DSTheme`. It follows system appearance and uses dynamic color on Android 12+; earlier devices use Material 3 light/dark defaults. For deterministic previews/tests, set `dynamicColor = false` and select `darkTheme` explicitly. Standard `MaterialTheme.colorScheme`, `typography`, and `shapes` remain available.

`DSSpacing` exposes `xs`, `sm`, `md`, `lg`, `xl`, and `xxl` (4–32 dp). These are content spacing values; native controls retain their own padding, shape, text scaling, and touch targets.

## Components

| API | Purpose | Caller owns |
| --- | --- | --- |
| `DSButton` | Primary, secondary, or destructive action; disabled/loading states | Callback, enabled/loading state, async work and failures |
| `DSTextField` | Labeled controlled field with supporting/error text | Value, validation, keyboard/IME configuration |
| `DSStatusView` | Neutral/empty or error message with optional action content | Copy, retry/navigation policy, action composition |

```kotlin
@Composable
fun ExampleForm(onContinue: () -> Unit) {
    var name by remember { mutableStateOf("") }
    DSTheme {
        Column(Modifier.padding(DSSpacing.lg)) {
            DSTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                supportingText = "Enter your name",
                singleLine = true,
            )
            DSButton("Continue", onClick = onContinue)
        }
    }
}
```

Import Compose runtime/layout APIs plus the design-system components and theme APIs used above. Supplied strings should come from the consuming app's localized resources.

A loading button keeps its visible/accessibility label, announces loading state, and disables activation. The callback is a synchronous notification; the component does not launch coroutines, retry, or deduplicate work already started by its caller. Android callers explicitly pass enabled state.

`DSTextField` forwards `KeyboardOptions`, `KeyboardActions`, and `singleLine`. Error text replaces supporting text and supplies error semantics. The library does not trim, normalize, or validate user input.

`DSStatusView` accepts optional composable action content, so callers can supply a `DSButton` or an appropriate native control. It exposes its title as a heading and communicates errors in text as well as color.

## Previews and verification

Open `src/debug/java/com/example/nativetemplate/designsystem/previews/DesignSystemPreviews.kt` in Android Studio. The gallery covers light/dark, large text, RTL, and component states; it is excluded from Release.

From `apps/android/`:

```sh
./gradlew :core:designsystem:testDebugUnitTest :core:designsystem:lintDebug
./gradlew :core:designsystem:assembleRelease
```

Component tests run real Compose semantics/interactions under Robolectric. They need no emulator; the first run downloads the pinned test dependencies and Android test runtime. Compilation and these tests do not substitute for device visual/accessibility inspection.

Use native controls directly when this small library does not cover the required behavior. Add a new shared component for a concrete reusable responsibility, and keep its behavior checks and examples alongside it.

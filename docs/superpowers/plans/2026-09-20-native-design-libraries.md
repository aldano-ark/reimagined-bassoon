# Native Design Libraries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Native execution is the established session preference; use one independent final review.

**Goal:** Add native Android and iOS design libraries with adaptive styling, three reusable component families, previews, real interaction tests, and documented APIs.

**Architecture:** Android exposes `:core:designsystem` as a Compose library; iOS exposes a local Swift package product `DesignSystem`. Components use platform-native controls and receive externally owned state/callbacks. The existing apps consume their libraries, while a debug-only iOS test host provides a place for XCTest to exercise package views without changing ordinary app behavior.

**Tech Stack:** Existing Kotlin/Compose/Material 3 and Gradle baseline; Robolectric 4.16.1 and JUnit 4.13.2 for Android component tests; Swift 6, SwiftUI, local Swift packages, XCTest UI automation; existing POSIX commands and Python command tests.

**Spec:** [Approved native design-library design](../specs/2026-09-20-native-design-libraries-design.md).

**Status:** Implemented with passing native checks; independent final review is pending. Android has 9 passing component tests, iOS has 8 passing interaction tests, and the shared commands have 20 passing tests.

## Global Constraints

- Android should feel native to Android, and iOS should feel native to iOS.
- The template still has no product domain or brand.
- Neither library depends on an app, feature, other platform, or root script for compilation.
- Build configuration must retain Android API 26 minimum, compile/target SDK 36, Java 17 bytecode, and iOS 17 minimum.
- Preserve the existing pinned dependencies, Gradle wrapper, and project-scoped Java 25 daemon criteria; use Android Studio's bundled JBR for verification.
- Components receive state and callbacks; they do not perform networking, validation business rules, navigation, or asynchronous work themselves.
- Public names use a short `DS` prefix, with platform-idiomatic parameter types and composition.
- Native controls retain their own minimum sizes, corner styles, typography, and internal padding.
- Do not wrap every native API or introduce a synthetic iOS theme merely to match Android.
- Ordinary app launches retain the `NativeTemplate` screen. Previews and the iOS test host are debug-only.
- Computer control remains with the user. Do not invoke computer-use tools or interact with their existing IDE/simulator session.
- Automated native tests may run via command line. iOS test execution uses a separately created test simulator and removes only that simulator afterward.
- Compilation, actual test execution, and visual/accessibility inspection are distinct claims. Record an unavailable check as unverified.

## Review Focus

1. A loading or explicitly disabled button must not activate, and loading must preserve its accessible name; test in Android and iOS component suites.
2. A parent-disabled SwiftUI button must stay disabled even when its own `isEnabled` argument is true; test in the iOS host.
3. A field must reflect caller-driven value replacement and prioritize error over supporting text without duplicate accessible labels; test in both suites.
4. Long labels under enlarged text and right-to-left layout must remain usable with native minimum targets; exercise Android layout tests and both platform previews, with an iOS large-text test fixture.
5. Verification must fail if library tests or test-target compilation fails; extend the real shell command tests before changing dispatch.

## File map and public interfaces

Android source package: `com.example.nativetemplate.designsystem`. All Kotlin paths below use `src/<source-set>/java/com/example/nativetemplate/designsystem/` as their prefix.

| Unit | Files | Responsibility |
| --- | --- | --- |
| Android library | `apps/android/core/designsystem/build.gradle.kts`, `src/main/AndroidManifest.xml` | Build boundary and native library configuration |
| Android foundations | `theme/DSTheme.kt`, `theme/DSSpacing.kt` | Material theme selection and logical spacing |
| Android components | `components/DSButton.kt`, `components/DSTextField.kt`, `components/DSStatusView.kt` | Native controls and state contracts |
| Android previews/tests | `src/debug/.../previews/DesignSystemPreviews.kt`, `src/test/.../DesignSystemTest.kt` | Discoverability and actual Compose behavior tests |
| iOS library | `apps/ios/Packages/Core/DesignSystem/Package.swift`, `Sources/DesignSystem/` | Independently consumable SwiftUI library |
| iOS foundations/components | `Foundations/DSSpacing.swift`, `Components/{DSButton,DSTextField,DSStatusView}.swift` | Native SwiftUI styling and composition |
| iOS resources/previews | `Resources/en.lproj/Localizable.strings`, `Previews/DesignSystemPreviews.swift` | Loading-state localization and debug previews |
| iOS test host/suite | `apps/ios/App/DesignSystemTestHost.swift`, `apps/ios/DesignSystemUITests/DesignSystemUITests.swift` | Debug-only UI fixtures and XCTest interactions |
| Integration | Native manifests/projects, app roots, version catalog | Consume real library products and preserve ordinary launches |
| Verification | `tooling/scripts/lib.sh`, `tooling/tests/test_commands.py` | Library test/lint inclusion and failure propagation |
| Documentation | Library READMEs, `docs/design-system/README.md`, contributor guides, verification record | Usage, native differences, and measured evidence |

The three tasks below form one cohesive design-library deliverable: the platform units are independently testable; the final command/documentation task consumes both.

## Task 1: Android library, components, tests, and integration

**Files:**

- Create the Android files listed in the file map and `src/main/res/values/strings.xml`.
- Modify `apps/android/settings.gradle.kts`, `apps/android/build.gradle.kts`, `apps/android/gradle/libs.versions.toml`, `apps/android/app/build.gradle.kts`, and the app's `MainActivity.kt`.
- Create `apps/android/core/designsystem/README.md`; update `apps/android/core/README.md`.

**Interfaces:** Produces `DSTheme`, `DSSpacing`, `DSButtonIntent`, `DSButton`, `DSTextField`, `DSStatusKind`, and `DSStatusView` in the package above. Produces `:core:designsystem:assembleDebug`, `:core:designsystem:lintDebug`, and `:core:designsystem:testDebugUnitTest`. The application artifact path stays unchanged.

- [x] **1. Establish a clean feature branch and read the approved spec.** Work in the existing dedicated checkout unless the user requests another worktree. Record the base commit and task progress using the execution skill. Keep the main branch available for the completed template. Confirm the current command suite passes before editing shared tooling.

- [x] **2. Register the library and test prerequisites.** Add the catalog plugin alias `android-library` with id `com.android.library` and existing `agp` version. Declare it `apply false` in the root build and include `:core:designsystem` in settings. Add catalog entries for Compose `ui`, `ui-tooling-preview`, `ui-tooling`, `ui-test-junit4`, and `ui-test-manifest` using the existing BOM, plus `junit:junit:4.13.2` and `org.robolectric:robolectric:4.16.1`.

The library build file is:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.nativetemplate.designsystem"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.activity.compose)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
```

`api` is intentional for Compose types exposed in public signatures. Add a minimal `<manifest />`. Do not create convention plugins solely for these two small modules. Configure test heap only if measured memory failures require it.

- [x] **3. Write failing component tests.** Use `@RunWith(RobolectricTestRunner::class)`, `@Config(sdk = [35])`, and `@get:Rule val compose = createComposeRule()`. Set content under `DSTheme(dynamicColor = false)`. Add the following real interaction tests before implementing the controls:

```kotlin
@Test fun enabledButtonActivates() {
    var calls = 0
    compose.setContent {
        DSTheme(dynamicColor = false) { DSButton("Save", onClick = { calls++ }) }
    }
    compose.onNodeWithText("Save").performTouchInput { click() }
    compose.runOnIdle { assertEquals(1, calls) }
}

@Test fun loadingButtonRetainsNameAndBlocksActivation() {
    var calls = 0
    compose.setContent {
        DSTheme(dynamicColor = false) {
            DSButton("Save", onClick = { calls++ }, loading = true)
        }
    }
    compose.onNodeWithText("Save").assertIsNotEnabled()
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Loading"))
        .performTouchInput { click() }
    compose.runOnIdle { assertEquals(0, calls) }
}

@Test fun fieldReflectsCallerOwnedState() {
    val value = mutableStateOf("")
    compose.setContent {
        DSTheme(dynamicColor = false) {
            DSTextField(value.value, { value.value = it }, label = "Name")
        }
    }
    compose.onNode(hasSetTextAction()).performTextInput("Ada")
    compose.runOnIdle { assertEquals("Ada", value.value); value.value = "Grace" }
    compose.onNode(hasSetTextAction()).assertTextContains("Grace")
}

@Test fun errorReplacesSupportingTextAndIsAccessible() {
    compose.setContent {
        DSTheme(dynamicColor = false) {
            DSTextField("", {}, label = "Name", supportingText = "Helpful text", errorText = "Required")
        }
    }
    compose.onNode(hasSetTextAction())
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Required"))
    compose.onNodeWithText("Helpful text").assertDoesNotExist()
}
```

Also write a disabled-button test with `enabled = false`, a loading-to-enabled transition using `mutableStateOf(true)`, and a status-action test that verifies its caller callback fires. For enlarged text/RTL, provide `LocalDensity` with font scale 2 and `LocalLayoutDirection` with `LayoutDirection.Rtl`; render a long button label in a 220 dp-wide parent. Assert the button's height is at least 48 dp and its label bounds fit within the button bounds. These checks exercise wrapper behavior and layout, not hardcoded token values.

Run `./gradlew :core:designsystem:testDebugUnitTest` from `apps/android/`. Expect initial compilation failure naming the missing DS APIs. Add only the minimum API declarations needed to compile the test harness and capture failing interaction assertions before implementing the actual controls. A compilation-only red result is not the final interaction proof.

- [x] **4. Implement foundations.** `DSSpacing` exposes `xs = 4.dp`, `sm = 8.dp`, `md = 12.dp`, `lg = 16.dp`, `xl = 24.dp`, and `xxl = 32.dp`. `DSTheme` follows system appearance and uses the native Material typography/shapes:

```kotlin
@Composable
fun DSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, typography = Typography(), shapes = Shapes(), content = content)
}
```

- [x] **5. Implement button behavior with native controls.** Public signature:

```kotlin
enum class DSButtonIntent { Primary, Secondary, Destructive }

@Composable
fun DSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    intent: DSButtonIntent = DSButtonIntent.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
)
```

Implement the body with Material `Button`, `OutlinedButton`, and destructive `ButtonDefaults.buttonColors(containerColor = colorScheme.error, contentColor = colorScheme.onError)`. Pass `enabled && !loading` to the native control. Use one shared `@Composable RowScope.() -> Unit` label slot containing the visible text and, while loading, a native `CircularProgressIndicator` with `LocalContentColor`, a small icon-sized indicator, and spacing. Remove the spinner's duplicate accessibility semantics with `clearAndSetSemantics {}`; add a localized `stateDescription` to the button only while loading. Define `design_system_loading` as `Loading` in library string resources. Do not add a fixed text height or replace the native clickable implementation.

- [x] **6. Implement controlled fields and status composition.** The text field wraps `OutlinedTextField`:

```kotlin
@Composable
fun DSTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
) {
    val message = errorText ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.then(
            if (errorText != null) Modifier.semantics { error(errorText) } else Modifier
        ),
        enabled = enabled,
        isError = errorText != null,
        supportingText = if (message != null) { { Text(message) } } else null,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
    )
}
```

`DSStatusKind` has `Neutral` and `Error`. `DSStatusView(title: String, modifier: Modifier = Modifier, description: String? = null, kind: DSStatusKind = Neutral, action: (@Composable () -> Unit)? = null)` lays out a `Column` with logical start alignment and native typography. Mark the title as a semantic heading; use `colorScheme.error` for an error title and `onSurface` otherwise. Render description with `onSurfaceVariant`, and invoke the optional action slot. No network or retry policy belongs here.

- [x] **7. Make the behavior tests green.** Run the complete library unit-test task and `:core:designsystem:lintDebug`. Investigate actual test/runtime failures before changing dependencies; do not turn behavior tests into constant assertions. Run the tests with the project's JBR 25 runtime first. If a test framework incompatibility is discovered, record the diagnostic and select a supported test-only remedy without changing the user's global Java environment.

- [x] **8. Add debug previews and connect the app.** Preview a stateful field, normal/disabled/loading buttons, all intents, and neutral/error status views. Add separate `@Preview` annotations for light/dark, font scale 2, and a right-to-left locale; the gallery explicitly disables dynamic color. The gallery is debug-only.

Add `implementation(project(":core:designsystem"))` to the app. Replace its `MaterialTheme` entry point with `DSTheme`, leaving the neutral screen content intact. Do not move app startup into the library.

- [x] **9. Verify and document Android.** Run:

```sh
./gradlew --no-daemon :app:assembleDebug :app:lintDebug \
  :core:designsystem:lintDebug :core:designsystem:testDebugUnitTest
./gradlew --no-daemon :core:designsystem:assembleRelease
```

Expect successful debug app/library builds, library tests, lint, and release library compilation. The release build proves preview-only dependencies have not leaked into required runtime code. Add library import/API examples, dynamic-color behavior, state ownership, and preview/test entry points to its README. Commit the Android deliverable as `feat(android): add native design system library`.

## Task 2: SwiftUI package, native UI tests, previews, and integration

**Files:**

- Create the iOS files in the file map and `apps/ios/Packages/Core/DesignSystem/README.md`.
- Modify `apps/ios/App/NativeTemplateApp.swift`, `apps/ios/App/ContentView.swift`, `apps/ios/NativeTemplate.xcodeproj/project.pbxproj`, and its shared `NativeTemplate.xcscheme`.
- Update `apps/ios/Packages/Core/README.md`.

**Interfaces:** Produces public `DSSpacing`, `DSButtonIntent`, `DSButton`, `DSTextField`, `DSStatusKind`, and generic `DSStatusView<Action: View>` in module `DesignSystem`. Produces an Xcode UI-test target named `DesignSystemUITests` in the existing shared scheme. Test-host launch arguments are `--design-system-tests` plus `--ds-scenario=buttons|field|status`; `--ds-large-rtl` applies enlarged text and RTL to the test host only. Normal launches and the application artifact path stay unchanged.

- [x] **1. Register the local package and native test target.** Package manifest:

```swift
// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "DesignSystem",
    defaultLocalization: "en",
    platforms: [.iOS(.v17)],
    products: [.library(name: "DesignSystem", targets: ["DesignSystem"])],
    targets: [
        .target(name: "DesignSystem", resources: [.process("Resources")])
    ]
)
```

Create a package source directory and localization resource before resolving it. Set `"design_system_loading" = "Loading";` in `Resources/en.lproj/Localizable.strings`. No empty test target is needed in the package: component interactions are tested in the app host.

Add an `XCLocalSwiftPackageReference` with path `Packages/Core/DesignSystem`, a `XCSwiftPackageProductDependency` for `DesignSystem`, and the product to the app's framework build phase. Use the existing project structure and stable IDs; do not regenerate the whole project. A one-time `xcodeproj` helper may edit it, but the normal build must not require Ruby.

Create the native UI-test bundle `DesignSystemUITests` with `TEST_TARGET_NAME = NativeTemplate`, deployment minimum 17, Swift language version 6, generated Info.plist, and bundle identifier `com.example.nativetemplate.DesignSystemUITests`. Add an app target dependency and register the target in the shared scheme's test action. Any explicit system framework references must use `SDKROOT`, preserving the previous review correction.

- [x] **2. Define the debug-only test host and write failing UI tests.** `DesignSystemTestHost` owns fixture state with `@State` and selects a scenario from the launch arguments. It is compiled only under `#if DEBUG`. The app entry point uses it only when `--design-system-tests` is present; otherwise it renders `ContentView`. No test switch exists in Release.

Fixtures:

- `buttons`: a `DSButton("Save", isLoading: loading) { count += 1 }` initially loading; a native `Button("Finish loading") { loading = false }`; `DSButton("Disabled", isEnabled: false) { count += 1 }`; `DSButton("Inherited disabled") { count += 1 }.disabled(true)`; and a visible `Text("Count: \(count)")` with accessibility identifier `activation-count`. Only DS actions increment the counter.
- `field`: `DSTextField("Name", text: $value, supportingText: "Helpful text", errorText: error)`, a native `Button("Set externally")` setting value to `Grace`, and `Button("Show error")` setting error to `Required`.
- `status`: an error `DSStatusView` with title `Could not load`, description `Try again`, and a `DSButton("Retry")` incrementing the same visible counter.

The `--ds-large-rtl` argument applies `.dynamicTypeSize(.accessibility3)` and `.environment(\.layoutDirection, .rightToLeft)` to the host, not the entire app. Use a long Save label in that variant to exercise wrapping and minimum target size.

Write XCTest methods annotated `@MainActor`, with `XCUIApplication` launched for each scenario. Example interaction tests:

```swift
@MainActor
func testLoadingButtonKeepsNameAndCannotActivate() {
    let app = XCUIApplication()
    app.launchArguments = ["--design-system-tests", "--ds-scenario=buttons"]
    app.launch()
    let save = app.buttons["Save"]
    XCTAssertTrue(save.waitForExistence(timeout: 5))
    XCTAssertFalse(save.isEnabled)
    XCTAssertEqual(save.value as? String, "Loading")
    save.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
    XCTAssertEqual(app.staticTexts["activation-count"].label, "Count: 0")
    app.buttons["Finish loading"].tap()
    XCTAssertTrue(save.isEnabled)
    save.tap()
    XCTAssertEqual(app.staticTexts["activation-count"].label, "Count: 1")
}

@MainActor
func testFieldUsesCallerStateAndExposesError() {
    let app = XCUIApplication()
    app.launchArguments = ["--design-system-tests", "--ds-scenario=field"]
    app.launch()
    let field = app.textFields["Name"]
    XCTAssertTrue(field.waitForExistence(timeout: 5))
    field.tap()
    field.typeText("Ada")
    XCTAssertEqual(field.value as? String, "Ada")
    app.buttons["Set externally"].tap()
    XCTAssertEqual(field.value as? String, "Grace")
    app.buttons["Show error"].tap()
    XCTAssertTrue(app.staticTexts["Required"].exists)
    XCTAssertFalse(app.staticTexts["Helpful text"].exists)
    XCTAssertEqual(app.textFields.matching(identifier: "Name").count, 1)
}
```

Add a disabled/inherited-disabled test verifying both buttons are disabled and coordinate taps do not increment the counter; a status Retry activation test; and an enlarged-text/RTL test verifying the long-label button exists, has a height of at least 44 points, and remains operable once loading ends. Do not assert Apple-specific pixel colors or exact native style geometry.

Run the full `build-for-testing` command in step 6 first and observe missing DS API compilation diagnostics. Add the minimum type/initializer declarations needed to compile the host and observe failing XCTest interaction assertions on the isolated simulator before implementing the actual controls. Successful test compilation alone does not establish behavior.

- [x] **3. Implement native SwiftUI foundations and buttons.** `DSSpacing` is a public enum with static `CGFloat` values named `xs`, `sm`, `md`, `lg`, `xl`, `xxl` matching 4, 8, 12, 16, 24, 32. Native SwiftUI text styles and semantic foreground/background colors remain available directly.

Button API:

```swift
public enum DSButtonIntent: Sendable, Equatable { case primary, secondary, destructive }

public struct DSButton: View {
    private let title: LocalizedStringKey
    private let intent: DSButtonIntent
    private let isEnabled: Bool
    private let isLoading: Bool
    private let action: () -> Void

    public init(
        _ title: LocalizedStringKey,
        intent: DSButtonIntent = .primary,
        isEnabled: Bool = true,
        isLoading: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.intent = intent
        self.isEnabled = isEnabled
        self.isLoading = isLoading
        self.action = action
    }

    @ViewBuilder public var body: some View {
        switch intent {
        case .primary: control.buttonStyle(.borderedProminent)
        case .secondary, .destructive: control.buttonStyle(.bordered)
        }
    }

    private var control: some View {
        Button(role: intent == .destructive ? .destructive : nil, action: action) {
            HStack(spacing: DSSpacing.sm) {
                if isLoading { ProgressView().accessibilityHidden(true) }
                Text(title).multilineTextAlignment(.center)
            }
            .frame(minHeight: 44)
        }
        .disabled(!isEnabled || isLoading)
        .accessibilityLabel(Text(title))
        .accessibilityValue(isLoading ? Text("design_system_loading", bundle: .module) : Text(""))
    }
}
```

Do not set a fixed tint or custom corner shape. The native destructive role and inherited tint/style behavior remain intact.

- [x] **4. Implement labeled fields and status views.** `DSTextField` is a public SwiftUI View with an explicit initializer accepting `LocalizedStringKey` label, `Binding<String>` text, `isEnabled`, optional supporting/error `String`, `UIKeyboardType`, optional `UITextContentType`, optional `TextInputAutocapitalization`, autocorrection flag, `SubmitLabel`, and an `onSubmit` closure. Defaults use normal native text editing; add no text transformations.

Its initializer interface is:

```swift
public init(
    _ label: LocalizedStringKey,
    text: Binding<String>,
    isEnabled: Bool = true,
    supportingText: String? = nil,
    errorText: String? = nil,
    keyboardType: UIKeyboardType = .default,
    contentType: UITextContentType? = nil,
    capitalization: TextInputAutocapitalization? = .sentences,
    autocorrectionDisabled: Bool = false,
    submitLabel: SubmitLabel = .return,
    onSubmit: @escaping () -> Void = {}
)
```

Store text using `@Binding private var text: String` and assign `self._text = text` in that initializer; store the remaining arguments unchanged. This signature is an interface specification, not a standalone initializer implementation.

Use a leading-aligned `VStack` containing a visible semantic-font label, `TextField`, and the selected message. Hide the separate visible label from accessibility and set the field's accessible label explicitly, so the label is announced once. Leave the supporting/error text as a separate accessible text element in reading order. Do not also duplicate it as an accessibility hint. Error text supersedes supporting text and uses the native error color treatment, while the message itself communicates the problem.

The editable control's modifier chain is:

```swift
TextField("", text: $text)
    .textFieldStyle(.roundedBorder)
    .accessibilityLabel(Text(label))
    .keyboardType(keyboardType)
    .textContentType(contentType)
    .textInputAutocapitalization(capitalization)
    .autocorrectionDisabled(autocorrectionDisabled)
    .submitLabel(submitLabel)
    .onSubmit(onSubmit)
    .disabled(!isEnabled)
    .frame(minHeight: 44)
```

Declare `DSStatusKind` with `.neutral` and `.error`. `DSStatusView<Action: View>` accepts a required `LocalizedStringKey` title, optional `String` description, kind defaulting to `.neutral`, and a `@ViewBuilder action: () -> Action`. Add a constrained initializer where `Action == EmptyView` for no-action usage. Use a leading-aligned `VStack`, native typography, `.accessibilityAddTraits(.isHeader)` on the title, semantic secondary styling for the description, and the composed action. Error titles use the platform error color; no icon dependency is required.

- [x] **5. Add previews and integrate the ordinary app.** Wrap the preview gallery and its `#Preview` declarations in `#if DEBUG`. Include normal and error controls, loading/disabled states, light/dark appearance, `.dynamicTypeSize(.accessibility3)`, and RTL layout. Use a private stateful preview wrapper for text entry.

Import `DesignSystem` in `ContentView` and use `.padding(DSSpacing.lg)` on the neutral screen. The test-host conditional is debug-only in `NativeTemplateApp`, and a normal launch continues to show `NativeTemplate`.

- [x] **6. Compile debug, test, and release configurations.** From the repository root, use the committed shared scheme:

```sh
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/ios CODE_SIGNING_ALLOWED=NO build-for-testing
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate -configuration Release \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/ios-release CODE_SIGNING_ALLOWED=NO build
```

Expect the package, normal app, previews, and UI-test bundle to compile in Debug, and a Release app without the debug test host/previews. If SwiftUI preview macros are blocked by sandbox plugin execution, use the authorized local build execution mechanism rather than deleting previews.

- [x] **7. Run XCTest in a separate simulator.** Inspect `xcrun simctl list runtimes available` and `xcrun simctl list devicetypes` and assign the observed installed iOS 27 runtime identifier to `NATIVE_IOS_RUNTIME` and an observed iPhone device-type identifier to `NATIVE_DEVICE_TYPE`. These are discovered environment values, not source constants. Create a uniquely named disposable simulator and retain its returned UUID:

```sh
NATIVE_DESIGN_TEST_DEVICE=$(xcrun simctl create \
  "NativeTemplate Design Tests $(date +%s)" "$NATIVE_DEVICE_TYPE" "$NATIVE_IOS_RUNTIME") || exit 1
test -n "$NATIVE_DESIGN_TEST_DEVICE" || exit 1
trap 'xcrun simctl shutdown "$NATIVE_DESIGN_TEST_DEVICE" >/dev/null 2>&1 || :; xcrun simctl delete "$NATIVE_DESIGN_TEST_DEVICE" >/dev/null 2>&1 || :' EXIT
mkdir -p .build/ios-design-results
```

In the same shell, run:

```sh
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate -configuration Debug \
  -destination "platform=iOS Simulator,id=$NATIVE_DESIGN_TEST_DEVICE" \
  -derivedDataPath .build/ios-design-tests \
  -resultBundlePath ".build/ios-design-results/$NATIVE_DESIGN_TEST_DEVICE.xcresult" \
  -parallel-testing-enabled NO -only-testing:DesignSystemUITests \
  CODE_SIGN_IDENTITY=- test
```

Allow standard simulator ad-hoc signing for test execution; this does not require a personal distribution identity. Capture the exit code and XCTest result bundle. Shut down and delete only the newly created UUID afterward, using a trap in the execution wrapper. Do not use `booted` or select the user's existing foreground simulator. If creation/execution is genuinely unavailable, keep the tests and report compiled versus executed status separately.

- [x] **8. Document and commit iOS.** Add local package usage, public initializer examples, native style/tint inheritance, state ownership, localization, preview instructions, and test-host/test-runner instructions to its README. Confirm `plutil -lint`, shared-scheme resolution, native builds, and test results. Commit as `feat(ios): add native SwiftUI design system`.

## Task 3: Shared verification, documentation, and isolation evidence

**Files:** Modify `tooling/scripts/lib.sh`, `tooling/tests/test_commands.py`, root/platform READMEs and `AGENTS.md`, `docs/architecture/overview.md`, and `docs/workflows/verification.md`. Create `docs/design-system/README.md` and `docs/workflows/design-libraries-verification-2026-09-20.md`.

**Interfaces:** Consumes the native targets from Tasks 1 and 2. `build android|ios|all` retains its existing contract. `verify android` now includes design-library lint and JVM component tests. `verify ios` compiles the UI-test target with `build-for-testing`; actual XCTest execution remains the separate simulator command documented above.

- [x] **1. Write failing command-contract tests.** Extend the existing fake Gradle tool to accept both old and new verify argument lists during the red step. Keep the real shell scripts under test. Assert all required tasks are present:

```python
def test_verify_includes_design_library_checks(self):
    self.assert_success(self.run_cli('verify', 'android'))
    calls = [call for call in self.calls() if call['tool'] == 'gradlew']
    self.assertEqual(len(calls), 1)
    self.assertIn(':core:designsystem:lintDebug', calls[0]['args'])
    self.assertIn(':core:designsystem:testDebugUnitTest', calls[0]['args'])

def test_ios_verify_compiles_test_target(self):
    self.assert_success(self.run_cli('verify', 'ios'))
    calls = [call for call in self.calls()
             if call['tool'] == 'xcodebuild' and call['args'] != ['-version']]
    self.assertEqual(calls[0]['args'][-1], 'build-for-testing')
```

In the fake Gradle tool, `NATIVE_FAKE_LIBRARY_TEST_STATUS` produces its requested failure only when the component test task is requested; add a test setting it to `74` and asserting `verify android` returns `74`. Add the corresponding iOS test-compilation failure case using the existing native exit-code mechanism. Run the full Python suite and observe these new expectations fail against current dispatch.

- [x] **2. Extend native verification without hiding failures.** In the Android verify branch append:

```sh
set -- "$@" :app:lintDebug :core:designsystem:lintDebug :core:designsystem:testDebugUnitTest
```

In the iOS branch select the build action before invoking Xcode:

```sh
native_xcode_action=build
if [ "$native_action" = verify ]; then
    native_xcode_action=build-for-testing
fi
```

Pass `"$native_xcode_action"` as the final Xcode argument. Preserve the existing project/scheme, output paths, artifact checks, logs, argument validation, platform isolation, and native status handling. Normal `build ios` must continue using `build`.

Update fake-tool expected arguments to require the final contracts after the new tests turn green. Run all command tests and `sh -n` on the shell files. Expected: prior behavior coverage remains green and missing library checks/test-target compilation would now fail the suite.

- [x] **3. Write discovery and usage documentation.** `docs/design-system/README.md` contains a platform API map, component state matrix, semantic styling differences, minimum-target/text-scaling guidance, preview locations, and exact checks. Include compilable usage examples using the interfaces specified in Tasks 1 and 2. State explicitly that native controls remain the escape hatch for uncovered patterns and that callers own state, validation, async work, navigation, and localization of their supplied copy.

Update contributor guidance to link the design libraries. Update command documentation: Android verification executes component tests; iOS verification compiles tests, while the separate simulator test command executes them. Keep HarmonyOS documentation-only. Record the internal English loading string's resource location for future localization.

- [x] **4. Run the integrated checks and prove isolation.** With command-scoped JBR 25 and the existing SDK environment, run `./tooling/scripts/verify all`. Run Android and iOS `build` commands concurrently from a directory outside the checkout, each with its own log, and verify both artifacts. Run the Python command suite after the final script changes. Compare tracked-file hashes before/after builds and inspect Git status so IDE/package resolution did not rewrite tracked configuration.

- [ ] **5. Record actual evidence and review.** The verification record lists exact component-test names/counts and outcomes, native build/lint results, debug/release/test-target compilation, XCTest simulator execution status, preview definitions, visual checks actually performed, independent/concurrent build results, and any unavailable checks. Do not claim a visual pass from preview compilation. Preserve the user's computer-control preference.

Use the established native-execution workflow's one independent final code review. Give the reviewer this plan/spec, exact commit range, evidence, and any recorded implementation rulings. Fix substantive issues with reproducing checks, then rerun only affected native checks plus the complete applicable test suite.

- [ ] **6. Commit and hand off.** Check Markdown links and `git diff --check`, commit the shared tooling/docs/evidence as `feat: integrate native design library verification`, and report the feature branch, commits, import paths, preview entry points, verified results, and remaining limitations. Integration into `main` is performed when requested by the user.

## Self-review and requirement coverage

| Requirement | Implementation and evidence |
| --- | --- |
| Separate native design libraries | Tasks 1–2, Gradle module/local package builds |
| Native appearance and adaptive styling | Native theme/styles in Tasks 1–2, preview variants |
| Three component families and external state ownership | Public APIs, interaction tests, usage examples |
| Disabled/loading behavior and accessible labels/errors | Android component suite and iOS XCTest host |
| Large text, RTL, minimum targets | Android layout test, iOS host variant, both preview sets |
| Existing app integration without product screens | DSTheme/package dependency, neutral ordinary app launch |
| Release exclusion of previews/test host | Android library Release and iOS app Release compilation |
| Reliable shared verification | Task 3 red/green command tests and real native checks |
| Human/agent discoverability | Library READMEs, shared guide, contributor links |
| Independent builds and clean inputs | Task 3 concurrent builds and tracked-file hashes |
| Respect user-controlled IDE session | No computer-use tools; isolated XCTest simulator |

## Execution handoff

Continue **native execution in this session**, preserving the established preference, with one independent final review. The Android and iOS units have distinct native test harnesses; shared verification and documentation depend on both. Review this written plan before implementation under the invoked planning workflow.

## Sources

- [Robolectric setup](https://robolectric.org/getting-started/)
- [Robolectric releases](https://github.com/robolectric/robolectric/releases/)
- [Compose design systems](https://developer.android.com/develop/ui/compose/designsystems)
- [SwiftUI view styles](https://developer.apple.com/documentation/swiftui/view-styles)
- [XCUIApplication](https://developer.apple.com/documentation/xcuiautomation/xcuiapplication)

Specific API names, task decomposition, and test-host behavior above are template implementation decisions. Native compilation and actual test execution must establish compatibility with the pinned toolchain.

# Native Environments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Recommended execution: implement in this session using executing-plans, then obtain one independent review. The user must review this plan and select or confirm the execution method before implementation.

**Goal:** Provide independently buildable dev, staging, and production native applications, each with its own identity and a working example of API request construction using native configuration.

**Architecture:** Android reads platform-local properties through Gradle product flavours; iOS reads platform-local xcconfig settings through schemes, build configurations, and a resolved Info.plist. App-owned immutable configuration feeds a small request factory and a read-only demonstration screen. Shared tooling verifies the variant matrix without becoming a native compilation dependency.

**Tech Stack:** Existing AGP 9.2.1, Gradle 9.4.1, Kotlin/Compose, JUnit 4.13.2, Swift 6, SwiftUI, XCTest, Xcode project files, POSIX shell, and Python standard-library tooling tests. Add no dependency versions or networking libraries.

**Spec:** [Approved native environment design](../../specs/2026-09-20-native-environments.md).

**Status:** Ready for plan review; all implementation steps remain unchecked.

## Global Constraints

- Both platforms use the canonical environment names `dev`, `stg`, and `prod`. Environment selection happens at build time.
- The user selected native configuration files and explicitly excluded `.env` files.
- Debug and Release builds of the same environment share its identity; there is no additional debug suffix.
- The example constructs a request but does not execute it or display an invented server response.
- The request factory receives a base URL explicitly; it does not read BuildConfig, Bundle, files, or global environment state itself.
- API base URLs must be absolute HTTPS URLs with a host and without embedded credentials, a query, or a fragment.
- A base path is allowed; appending `health` must preserve that path and handle a trailing slash consistently.
- Keep the existing single-platform-argument interface for `doctor`, `build`, and `verify`.
- No build rewrites tracked configuration files or globally selects an SDK.
- Missing tools or unrun checks remain unverified.
- Preserve Android API 26 minimum, compile/target SDK 36, build tools 36.0.0, Java 17 bytecode, iOS 17 minimum, all pinned versions, and native build independence.
- Keep the Kotlin namespace and Swift module name unchanged. Core design libraries receive no environment dependencies.
- Preserve the project's prior workflow of command-line automation and dedicated test devices; leave existing personal IDE and simulator sessions alone.
- Configuration files contain public values. Signing material, device teams, and machine paths stay out of tracked files.

| Environment | ID | Name | API base URL |
| --- | --- | --- | --- |
| dev | com.example.nativetemplate.dev | NativeTemplate Dev | https://dev-api.example.com |
| stg | com.example.nativetemplate.stg | NativeTemplate Stg | https://stg-api.example.com |
| prod | com.example.nativetemplate | NativeTemplate | https://api.example.com |

## Review Focus

1. A URL with a base path, port, or encoded slash must retain that information when `health` is appended; native request-factory tests cover this in Tasks 1 and 2.
2. Blank values, whitespace, malformed URLs, credentials, query strings, and fragments must produce explicit errors without environment fallback; configuration tests and the Gradle negative probe cover this in Tasks 1 and 2.
3. Xcode comment parsing or higher-precedence target settings must not truncate HTTPS URLs or select the wrong identity; Task 2 checks resolved build settings/plists, and Task 3 tests mismatched metadata.
4. A staging Release build must remain staging, and the iOS Debug fixture must remain Debug-only; compiled configuration tests, scheme checks, and runtime evidence cover this in Tasks 1–4.
5. A failed or incomplete later variant must not pass because another variant or an older artifact exists; Task 3 tests late failures, missing artifacts, and metadata mismatches.

## File map and ownership

All work is one feature. Tasks 1 and 2 own disjoint native files; Task 3 owns shared dispatch and tests and consumes both native interfaces. Task 4 records integration evidence and updates current guidance. Use an isolated checkout according to using-git-worktrees at execution time; do not create another checkout solely to write this plan.

| Unit | Exact paths / file family | Responsibility |
| --- | --- | --- |
| Android settings | `apps/android/config/dev.properties`, `stg.properties`, `prod.properties`; `apps/android/app/build.gradle.kts` | Public configuration and six app variants |
| Android configuration | `apps/android/app/src/main/java/com/example/nativetemplate/configuration/AppConfig.kt` | Environment enum, immutable values, validation, BuildConfig adapter |
| Android requests | `apps/android/app/src/main/java/com/example/nativetemplate/network/ApiRequestFactory.kt` | Native method-and-URI request value and health request construction |
| Android UI | `apps/android/app/src/main/java/com/example/nativetemplate/EnvironmentScreen.kt`, `MainActivity.kt`; `apps/android/app/src/main/res/values/strings.xml` | Display injected values using native text and existing theme/spacing |
| Android tests | `apps/android/app/src/test/java/com/example/nativetemplate/configuration/AppConfigTest.kt`, `BuildConfigurationTest.kt`; `apps/android/app/src/test/java/com/example/nativetemplate/network/ApiRequestFactoryTest.kt` | Validation, selected compiled values, and request behavior |
| iOS settings | `apps/ios/Config/Base.xcconfig`, `Debug.xcconfig`, `Release.xcconfig`, `Dev.xcconfig`, `Stg.xcconfig`, `Prod.xcconfig`, and six `Debug-Dev`/`Release-Dev`/`Debug-Stg`/`Release-Stg`/`Debug-Prod`/`Release-Prod` `.xcconfig` files | Compose native environment and build-mode settings |
| iOS wiring | `apps/ios/NativeTemplate.xcodeproj/project.pbxproj`; `apps/ios/NativeTemplate.xcodeproj/xcshareddata/xcschemes/NativeTemplate-{Dev,Stg,Prod}.xcscheme`; remove former `NativeTemplate.xcscheme` | Matching target configurations, three schemes, and app unit-test target |
| iOS configuration | `apps/ios/App/Configuration/AppConfig.swift`; `apps/ios/App/Resources/Info.plist` | Validated immutable configuration from expanded plist |
| iOS requests/UI | `apps/ios/App/Networking/ApiRequestFactory.swift`; `apps/ios/App/ContentView.swift`, `NativeTemplateApp.swift` | Build a URLRequest and display injected data or a configuration error |
| iOS tests | `apps/ios/AppTests/AppConfigTests.swift`, `ApiRequestFactoryTests.swift` | XCTest app-hosted validation and request tests |
| Shared commands | `tooling/scripts/lib.sh` | Default dev build, verification matrices, artifact/metadata checks |
| Shared tests | `tooling/tests/test_commands.py`; new `tooling/tests/test_environment_configuration.py` | Fake native dispatch, failure behavior, and cross-platform configuration parity |
| Current guides | `apps/android/README.md`, `apps/ios/README.md`, `apps/ios/Packages/Core/DesignSystem/README.md`, `docs/architecture/overview.md`, `docs/design-system/README.md`, `docs/workflows/development.md`, `docs/workflows/verification.md` | Correct commands, defaults, example behavior, rename instructions |
| Decision/evidence | `docs/decisions/0002-native-environment-configuration.md`; `docs/workflows/native-environments-verification-2026-09-20.md` | Chosen boundary and measured results/limitations |

Keep historical specs/plans and verification records unchanged except the approved status of this feature's spec. Search current guidance for obsolete scheme/artifact references during Task 4.

## Task 1: Android flavours, configuration, and request example

**Files:** Android files in the map and `apps/android/README.md`.

**Consumes:** Existing `DSTheme`, `DSSpacing`, app namespace, and `libs.junit` catalog entry.

**Produces:**

```kotlin
// AppConfig.kt, package com.example.nativetemplate.configuration
internal enum class AppEnvironment(val value: String) { DEV("dev"), STG("stg"), PROD("prod") }
// AppConfig: environment: AppEnvironment, applicationId: String,
// displayName: String, apiBaseUrl: java.net.URI.
// AppConfig.parse(environment: String, applicationId: String,
//   displayName: String, apiBaseUrl: String): AppConfig
// AppConfig.fromBuildConfig(): AppConfig

// ApiRequestFactory.kt, package com.example.nativetemplate.network
internal data class ApiRequest(val method: String, val uri: java.net.URI)
// ApiRequestFactory(baseUrl: java.net.URI).healthRequest(): ApiRequest
```

Gradle tasks follow `:app:assemble{Dev,Stg,Prod}{Debug,Release}`, `:app:lint{Dev,Stg,Prod}{Debug,Release}`, and `:app:test{Dev,Stg,Prod}{Debug,Release}UnitTest`. Debug APKs use `app/build/outputs/apk/<env>/debug/app-<env>-debug.apk`; unsigned Release APKs use `app/build/outputs/apk/<env>/release/app-<env>-release-unsigned.apk`.

- [ ] **1. Establish baseline and execution isolation.** Read both approved documents, root/platform instructions, and development/verification guides. Record the base commit and clean/dirty state. Run `python3 -m unittest discover -s tooling/tests -v` before modifying shared files. Run both doctors and record actual statuses. Do not infer native build success from doctor output.

- [ ] **2. Add the existing JUnit dependency and write failing pure configuration tests.** Add `testImplementation(libs.junit)` in the app. Before adding flavours, run the new pure tests with the existing `:app:testDebugUnitTest` task. Cover all keys and every invalid input below with assertions on the offending key, not exception existence alone.

```kotlin
@Test fun stagingConfigPreservesItsValues() {
    val config = AppConfig.parse("stg", "com.example.nativetemplate.stg",
        "NativeTemplate Stg", "https://stg-api.example.com")
    assertEquals(AppEnvironment.STG, config.environment)
    assertEquals("com.example.nativetemplate.stg", config.applicationId)
    assertEquals("NativeTemplate Stg", config.displayName)
    assertEquals("https://stg-api.example.com", config.apiBaseUrl.toString())
}

@Test fun invalidEndpointsAreRejected() {
    listOf("", " ", "/relative", "https://", "http://api.example.com",
        "https://user:password@api.example.com", "https://api.example.com?q=1",
        "https://api.example.com#fragment", "https://api.example.com/%zz",
        " https://api.example.com", "https://api.example.com/a b").forEach { raw ->
        val error = assertThrows(IllegalArgumentException::class.java) {
            AppConfig.parse("dev", "com.example.nativetemplate.dev", "Dev", raw)
        }
        assertTrue(error.message.orEmpty().contains("API_BASE_URL"))
    }
}
```

Use JUnit imports `org.junit.Test` and `org.junit.Assert.*`. Add table cases for unknown environment `qa`, whitespace-only identity/name, and an environment containing surrounding whitespace. Expected first run: compilation fails because `AppConfig` does not exist. Record that as the intended red step.

- [ ] **3. Implement the pure configuration model.** Keep parsing independent of Android APIs. Required values must be nonblank, have no surrounding whitespace or control characters; reject whitespace anywhere in URLs. Parse the URI with a key-specific exception, check HTTPS case-insensitively, require a host, and reject user info/query/fragment. Do not include credentials or an invalid URL's full value in an error message.

```kotlin
internal data class AppConfig private constructor(
    val environment: AppEnvironment,
    val applicationId: String,
    val displayName: String,
    val apiBaseUrl: URI,
) {
    companion object {
        fun parse(environment: String, applicationId: String,
                  displayName: String, apiBaseUrl: String): AppConfig {
            fun checked(key: String, value: String): String {
                require(value.isNotBlank() && value == value.trim() &&
                    value.none { it.isISOControl() }) { "Invalid $key" }
                return value
            }
            val environmentValue = checked("APP_ENVIRONMENT", environment)
            val selected = AppEnvironment.entries.singleOrNull { it.value == environmentValue }
            requireNotNull(selected) { "Invalid APP_ENVIRONMENT" }
            checked("APP_ID", applicationId)
            checked("APP_DISPLAY_NAME", displayName)
            checked("API_BASE_URL", apiBaseUrl)
            require(apiBaseUrl.none { it.isWhitespace() }) { "Invalid API_BASE_URL" }
            val uri = try { URI(apiBaseUrl) } catch (_: Exception) {
                throw IllegalArgumentException("Invalid API_BASE_URL")
            }
            require(uri.isAbsolute && uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
                uri.rawQuery == null && uri.rawFragment == null) { "Invalid API_BASE_URL" }
            return AppConfig(selected, applicationId, displayName, uri)
        }
    }
}
```

Include `import java.net.URI` and the enum from the interface. Run `./gradlew --no-daemon :app:testDebugUnitTest` from `apps/android`; expect the new configuration tests to pass before wiring BuildConfig.

- [ ] **4. Write failing request tests, then implement the request factory.** Test the actual method and URI, including encoded paths without double encoding:

```kotlin
@Test fun healthRequestPreservesBasePathAndEncoding() {
    listOf(
        "https://stg-api.example.com" to "https://stg-api.example.com/health",
        "https://stg-api.example.com/" to "https://stg-api.example.com/health",
        "https://api.example.com/v1" to "https://api.example.com/v1/health",
        "https://api.example.com/v1/" to "https://api.example.com/v1/health",
        "https://api.example.com:8443/a%2Fb/" to "https://api.example.com:8443/a%2Fb/health",
    ).forEach { (base, expected) ->
        val request = ApiRequestFactory(URI(base)).healthRequest()
        assertEquals("GET", request.method)
        assertEquals(expected, request.uri.toASCIIString())
    }
}
```

Run red, then add the implementation and rerun green:

```kotlin
internal class ApiRequestFactory(private val baseUrl: URI) {
    fun healthRequest(): ApiRequest = ApiRequest(
        method = "GET",
        uri = URI(baseUrl.toASCIIString().trimEnd('/') + "/health"),
    )
}
```

The constructor consumes a validated base URI from AppConfig. There is no transport, asynchronous work, network permission, or fallback URL.

- [ ] **5. Add native environment files.** Use these exact four keys; files use one literal `KEY=value` per line, UTF-8, and no interpolation or inline comments. Keep that simple format documented for parity/artifact checks.

```properties
# apps/android/config/dev.properties
APP_ENVIRONMENT=dev
APP_ID=com.example.nativetemplate.dev
APP_DISPLAY_NAME=NativeTemplate Dev
API_BASE_URL=https://dev-api.example.com
```

```properties
# apps/android/config/stg.properties
APP_ENVIRONMENT=stg
APP_ID=com.example.nativetemplate.stg
APP_DISPLAY_NAME=NativeTemplate Stg
API_BASE_URL=https://stg-api.example.com
```

```properties
# apps/android/config/prod.properties
APP_ENVIRONMENT=prod
APP_ID=com.example.nativetemplate
APP_DISPLAY_NAME=NativeTemplate
API_BASE_URL=https://api.example.com
```

- [ ] **6. Load and validate files in Gradle, then register flavours.** Add imports for `java.net.URI` and `java.util.Properties` above the existing plugins block, then this configuration loader after plugins. Errors name the file/key; invalid input never selects a fallback environment.

```kotlin
val environmentConfigurations = listOf("dev", "stg", "prod").associateWith { name ->
    val source = rootProject.file("config/$name.properties")
    if (!source.isFile) throw GradleException("Missing environment configuration: $source")
    val values = Properties().apply {
        source.reader(Charsets.UTF_8).use { load(it) }
    }
    fun required(key: String): String {
        val value = values.getProperty(key)
            ?: throw GradleException("$source: missing $key")
        if (value.isBlank() || value != value.trim() || value.any { it.isISOControl() }) {
            throw GradleException("$source: invalid $key")
        }
        return value
    }
    if (required("APP_ENVIRONMENT") != name) {
        throw GradleException("$source: APP_ENVIRONMENT must match $name")
    }
    if (!required("APP_ID").matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))) {
        throw GradleException("$source: invalid APP_ID")
    }
    required("APP_DISPLAY_NAME")
    val raw = required("API_BASE_URL")
    val uri = try { URI(raw) } catch (_: Exception) {
        throw GradleException("$source: invalid API_BASE_URL")
    }
    if (raw.any { it.isWhitespace() } || !uri.isAbsolute ||
        !uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() ||
        uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) {
        throw GradleException("$source: invalid API_BASE_URL")
    }
    values
}
if (environmentConfigurations.values.map { it.getProperty("APP_ID") }.distinct().size != 3) {
    throw GradleException("config/: APP_ID must be distinct for dev, stg, and prod")
}
```

Add this flavour wiring inside the existing `android` block. Escape backslashes and quotes in generated Java literals:

```kotlin
buildFeatures { compose = true; buildConfig = true }
flavorDimensions += "environment"
productFlavors {
    environmentConfigurations.forEach { (name, values) ->
        create(name) {
            dimension = "environment"
            applicationId = values.getProperty("APP_ID")
            resValue("string", "app_name", values.getProperty("APP_DISPLAY_NAME"))
            for (key in listOf("APP_ENVIRONMENT", "APP_DISPLAY_NAME", "API_BASE_URL")) {
                val escaped = values.getProperty(key).replace("\\", "\\\\").replace("\"", "\\\"")
                buildConfigField("String", key, "\"$escaped\"")
            }
        }
    }
}
testOptions.unitTests.all {
    it.systemProperty("native.config.dir", rootProject.file("config").absolutePath)
}
```

Remove the old `app_name` declaration from the main strings file to avoid a duplicate generated resource. Do not add a debug identity suffix or configure release signing. Existing Java/SDK/compiler/library settings remain intact.

- [ ] **7. Add the compiled configuration test before its BuildConfig adapter.** `BuildConfigurationTest` loads the selected properties file using the system property above and compares it to the values actually compiled for that variant:

```kotlin
@Test fun compiledConfigurationMatchesSelectedNativeFile() {
    val values = Properties().apply {
        File(System.getProperty("native.config.dir"), "${BuildConfig.FLAVOR}.properties")
            .reader(Charsets.UTF_8).use { load(it) }
    }
    val config = AppConfig.fromBuildConfig()
    assertEquals(values.getProperty("APP_ENVIRONMENT"), config.environment.value)
    assertEquals(values.getProperty("APP_ID"), config.applicationId)
    assertEquals(values.getProperty("APP_DISPLAY_NAME"), config.displayName)
    assertEquals(values.getProperty("API_BASE_URL"), config.apiBaseUrl.toString())
    val expectedId = "com.example.nativetemplate" +
        if (BuildConfig.FLAVOR == "prod") "" else ".${BuildConfig.FLAVOR}"
    assertEquals(expectedId, config.applicationId)
}
```

Import `java.io.File`, `java.util.Properties`, and the app's BuildConfig. Run `:app:testDevDebugUnitTest`; expect an unresolved adapter. Add this companion method and rerun:

```kotlin
fun fromBuildConfig(): AppConfig = parse(
    BuildConfig.APP_ENVIRONMENT, BuildConfig.APPLICATION_ID,
    BuildConfig.APP_DISPLAY_NAME, BuildConfig.API_BASE_URL,
)
```

Run app unit tests for all six variants. Release tests are intentional: they establish that the values compiled into release-mode app code retain their environment, while APK metadata checks separately establish identity/name.

- [ ] **8. Wire the read-only demonstration screen.** Create `EnvironmentScreen(config: AppConfig, request: ApiRequest)` with a scrollable Compose Column, `DSSpacing.lg`, native Material text styles, and wrapping text. In MainActivity construct `val config = AppConfig.fromBuildConfig()` and `val request = ApiRequestFactory(config.apiBaseUrl).healthRequest()` once before `setContent`; pass them through the existing DSTheme/Surface. Use these string resources for the display:

```xml
<resources>
    <string name="environment_label">Environment: %1$s</string>
    <string name="api_base_url_label">API base URL: %1$s</string>
    <string name="request_label">%1$s %2$s</string>
</resources>
```

The screen body uses `Text(config.displayName)`, `Text(stringResource(R.string.environment_label, config.environment.value))`, `Text(stringResource(R.string.api_base_url_label, config.apiBaseUrl.toString()))`, and `Text(stringResource(R.string.request_label, request.method, request.uri.toString()))`. Keep safe-drawing padding and avoid fixed heights, ellipsis, transport buttons, or a response panel. Use runtime inspection in Task 4 rather than tests that merely duplicate these static labels.

- [ ] **9. Exercise invalid native configuration and build the matrix.** In a disposable copy of `apps/android` excluding `build`, `.gradle`, and `.kotlin`, remove the staging API key and run `./gradlew --no-daemon :app:assembleStgDebug`; require nonzero exit with the file and `API_BASE_URL`. Restore only the disposable file and repeat with `http://api.example.com`; require nonzero again. Do not intentionally corrupt the user's checkout. Run the real six `assemble` and `lint` tasks plus six app test tasks. Until Task 3 lands, the old root wrapper's artifact paths are obsolete; use native commands and record this temporary integration dependency.

- [ ] **10. Update Android usage and commit this deliverable.** Explain config locations, dev default planned for shared commands, the six variants, endpoint editing/rebuild, release unsigned APK paths, and rename locations. Verify only the task-owned diff before a local commit such as `feat(android): add native environment flavours and request example`.

## Task 2: iOS configurations, app tests, and request example

**Files:** iOS files in the map, `apps/ios/README.md`, and the iOS design-system README's commands.

**Consumes:** Existing application target/module `NativeTemplate`, `DesignSystem` package, and debug-only `DesignSystemTestHost`.

**Produces:** Three shared schemes and six configurations; `AppTests` unit-test target; these internal Swift interfaces:

```swift
enum AppEnvironment: String, CaseIterable { case dev, stg, prod }
// AppConfig properties: environment: AppEnvironment, applicationID: String,
// displayName: String, apiBaseURL: URL.
// static AppConfig.load(info: [String: Any]) throws -> AppConfig
// static AppConfig.load(bundle: Bundle = .main) throws -> AppConfig
// enum ConfigurationError: LocalizedError, Equatable {
//   case missingValue(String), invalidValue(String)
// }
// ApiRequestFactory(baseURL: URL).healthRequest() -> URLRequest
```

App product/executable remains `NativeTemplate.app` / `NativeTemplate`; simulator outputs use `.build/ios/Build/Products/<Configuration>-iphonesimulator/NativeTemplate.app`.

- [ ] **1. Add an app-hosted XCTest target and write failing configuration tests.** Create `AppTests` with product type `com.apple.product-type.bundle.unit-test`, source/framework/resource build phases, dependency and container proxy to `NativeTemplate`, product reference, group, and source references. For the initial Debug/Release configurations use Swift 6, deployment target 17, generated test plist, `TEST_HOST = $(BUILT_PRODUCTS_DIR)/NativeTemplate.app/NativeTemplate`, and `BUNDLE_LOADER = $(TEST_HOST)`. Add the test to the current shared scheme for the red run; Task 2 step 7 replaces that scheme. Its scheme BuildAction entry is enabled for testing only, not Run/Profile/Archive/Analyze.

Use `import XCTest` and `@testable import NativeTemplate`. Start with a complete test fixture:

```swift
final class AppConfigTests: XCTestCase {
    private var validInfo: [String: Any] {
        ["AppEnvironment": "stg",
         "CFBundleIdentifier": "com.example.nativetemplate.stg",
         "CFBundleDisplayName": "NativeTemplate Stg",
         "APIBaseURL": "https://stg-api.example.com"]
    }

    func testStagingConfiguration() throws {
        let config = try AppConfig.load(info: validInfo)
        XCTAssertEqual(config.environment, .stg)
        XCTAssertEqual(config.applicationID, "com.example.nativetemplate.stg")
        XCTAssertEqual(config.displayName, "NativeTemplate Stg")
        XCTAssertEqual(config.apiBaseURL.absoluteString, "https://stg-api.example.com")
    }

    func testMissingValuesDoNotFallBack() {
        for key in validInfo.keys {
            var info = validInfo
            info.removeValue(forKey: key)
            XCTAssertThrowsError(try AppConfig.load(info: info)) { error in
                XCTAssertEqual(error as? ConfigurationError, .missingValue(key))
            }
        }
    }

    func testInvalidEndpointsAreRejected() {
        for value in ["", " ", "/relative", "https://", "http://api.example.com",
                      "https://user:password@api.example.com", "https://api.example.com?q=1",
                      "https://api.example.com#fragment", "https://api.example.com/%zz",
                      " https://api.example.com", "https://api.example.com/a b"] {
            var info = validInfo
            info["APIBaseURL"] = value
            XCTAssertThrowsError(try AppConfig.load(info: info)) { error in
                XCTAssertEqual(error as? ConfigurationError, .invalidValue("APIBaseURL"))
            }
        }
    }
}
```

Add cases for `qa`, nonstring plist values, blank names/IDs, and a whitespace-padded environment. Run `xcodebuild -project apps/ios/NativeTemplate.xcodeproj -scheme NativeTemplate -configuration Debug -destination 'generic/platform=iOS Simulator' -derivedDataPath .build/ios CODE_SIGNING_ALLOWED=NO build-for-testing`; expect missing AppConfig symbols, not an unrelated project-file error.

- [ ] **2. Implement validated AppConfig and ConfigurationError.** Use a private memberwise initializer and the interfaces above. Reject nonstring/absent entries with `.missingValue(key)`; reject empty/padded/control-character values with `.invalidValue(key)`. Environment must exactly match the enum. Use `URL(string: raw, encodingInvalidCharacters: false)` on the existing iOS 17 floor and URLComponents to require HTTPS/host and reject user/password/query/fragment. Reject raw whitespace before parsing. Errors name the invalid key and tell the developer to update configuration and rebuild; they do not include input credentials or substitute a default.

```swift
enum ConfigurationError: LocalizedError, Equatable {
    case missingValue(String)
    case invalidValue(String)

    var errorDescription: String? {
        switch self {
        case .missingValue(let key):
            return "Missing configuration value: \(key). Update configuration and rebuild."
        case .invalidValue(let key):
            return "Invalid configuration value: \(key). Update configuration and rebuild."
        }
    }
}
```

Use the following model beside the enum/error definitions; the private initializer keeps unvalidated values out of ordinary app construction:

```swift
import Foundation

struct AppConfig {
    let environment: AppEnvironment
    let applicationID: String
    let displayName: String
    let apiBaseURL: URL

    private init(environment: AppEnvironment, applicationID: String,
                 displayName: String, apiBaseURL: URL) {
        self.environment = environment
        self.applicationID = applicationID
        self.displayName = displayName
        self.apiBaseURL = apiBaseURL
    }

    static func load(bundle: Bundle = .main) throws -> AppConfig {
        try load(info: bundle.infoDictionary ?? [:])
    }

    static func load(info: [String: Any]) throws -> AppConfig {
        func required(_ key: String) throws -> String {
            guard let value = info[key] as? String else {
                throw ConfigurationError.missingValue(key)
            }
            guard !value.isEmpty,
                  value == value.trimmingCharacters(in: .whitespacesAndNewlines),
                  value.rangeOfCharacter(from: .controlCharacters) == nil else {
                throw ConfigurationError.invalidValue(key)
            }
            return value
        }
        let rawEnvironment = try required("AppEnvironment")
        guard let environment = AppEnvironment(rawValue: rawEnvironment) else {
            throw ConfigurationError.invalidValue("AppEnvironment")
        }
        let applicationID = try required("CFBundleIdentifier")
        let displayName = try required("CFBundleDisplayName")
        let rawURL = try required("APIBaseURL")
        guard rawURL.rangeOfCharacter(from: .whitespacesAndNewlines) == nil,
              let url = URL(string: rawURL, encodingInvalidCharacters: false),
              let parts = URLComponents(url: url, resolvingAgainstBaseURL: false),
              parts.scheme?.lowercased() == "https", let host = parts.host, !host.isEmpty,
              parts.user == nil, parts.password == nil,
              parts.query == nil, parts.fragment == nil else {
            throw ConfigurationError.invalidValue("APIBaseURL")
        }
        return AppConfig(environment: environment, applicationID: applicationID,
                         displayName: displayName, apiBaseURL: url)
    }
}
```

Add source references/build membership to the app. Rebuild for testing, then run AppTests on a dedicated simulator using the procedure in Task 4. Compilation alone is not the green XCTest result.

- [ ] **3. Write the request test red, then implement the factory and execute green.** Use this test table in `ApiRequestFactoryTests`; no URLSession or remote service is needed:

```swift
func testHealthRequestPreservesBasePathAndEncoding() throws {
    let cases = [
        ("https://stg-api.example.com", "https://stg-api.example.com/health"),
        ("https://stg-api.example.com/", "https://stg-api.example.com/health"),
        ("https://api.example.com/v1", "https://api.example.com/v1/health"),
        ("https://api.example.com/v1/", "https://api.example.com/v1/health"),
        ("https://api.example.com:8443/a%2Fb/", "https://api.example.com:8443/a%2Fb/health"),
    ]
    for (raw, expected) in cases {
        let base = try XCTUnwrap(URL(string: raw))
        let request = ApiRequestFactory(baseURL: base).healthRequest()
        XCTAssertEqual(request.httpMethod, "GET")
        XCTAssertEqual(request.url?.absoluteString, expected)
        XCTAssertNil(request.httpBody)
    }
}
```

```swift
import Foundation

struct ApiRequestFactory {
    let baseURL: URL

    func healthRequest() -> URLRequest {
        var request = URLRequest(url: baseURL.appendingPathComponent("health", isDirectory: false))
        request.httpMethod = "GET"
        return request
    }
}
```

Keep the tested path/encoding contract if the installed Foundation implementation requires an explicit URLComponents append. No response is fabricated and no request is sent.

- [ ] **4. Add native environment leaf files and mode composition.** `Dev.xcconfig`, `Stg.xcconfig`, and `Prod.xcconfig` contain exactly these values. `$()` deliberately expands to empty so the source contains no `//` comment delimiter inside an HTTPS URL.

```xcconfig
// Dev.xcconfig
APP_ENVIRONMENT = dev
APP_ID = com.example.nativetemplate.dev
APP_DISPLAY_NAME = NativeTemplate Dev
API_BASE_URL = https:/$()/dev-api.example.com
```

```xcconfig
// Stg.xcconfig
APP_ENVIRONMENT = stg
APP_ID = com.example.nativetemplate.stg
APP_DISPLAY_NAME = NativeTemplate Stg
API_BASE_URL = https:/$()/stg-api.example.com
```

```xcconfig
// Prod.xcconfig
APP_ENVIRONMENT = prod
APP_ID = com.example.nativetemplate
APP_DISPLAY_NAME = NativeTemplate
API_BASE_URL = https:/$()/api.example.com
```

`Base.xcconfig` sets `IPHONEOS_DEPLOYMENT_TARGET = 17.0` and `SWIFT_VERSION = 6.0`. Mode files include Base and hold the existing mode-dependent values removed from the project configuration dictionaries:

```xcconfig
// Debug.xcconfig
#include "Base.xcconfig"
SWIFT_ACTIVE_COMPILATION_CONDITIONS = $(inherited) DEBUG
SWIFT_OPTIMIZATION_LEVEL = -Onone
GCC_OPTIMIZATION_LEVEL = 0
GCC_PREPROCESSOR_DEFINITIONS = $(inherited) DEBUG=1
ENABLE_TESTABILITY = YES
ONLY_ACTIVE_ARCH = YES
DEBUG_INFORMATION_FORMAT = dwarf
MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE
```

```xcconfig
// Release.xcconfig
#include "Base.xcconfig"
SWIFT_OPTIMIZATION_LEVEL = -O
SWIFT_COMPILATION_MODE = wholemodule
ENABLE_TESTABILITY = NO
ENABLE_NS_ASSERTIONS = NO
DEBUG_INFORMATION_FORMAT = dwarf-with-dsym
MTL_ENABLE_DEBUG_INFO = NO
```

Each configuration-specific file consists of its two include lines:

| File | First line | Second line |
| --- | --- | --- |
| Debug-Dev.xcconfig | `#include "Debug.xcconfig"` | `#include "Dev.xcconfig"` |
| Release-Dev.xcconfig | `#include "Release.xcconfig"` | `#include "Dev.xcconfig"` |
| Debug-Stg.xcconfig | `#include "Debug.xcconfig"` | `#include "Stg.xcconfig"` |
| Release-Stg.xcconfig | `#include "Release.xcconfig"` | `#include "Stg.xcconfig"` |
| Debug-Prod.xcconfig | `#include "Debug.xcconfig"` | `#include "Prod.xcconfig"` |
| Release-Prod.xcconfig | `#include "Release.xcconfig"` | `#include "Prod.xcconfig"` |

- [ ] **5. Add an explicit app plist and migrate build configurations.** The new plist supplies custom keys; retain `GENERATE_INFOPLIST_FILE = YES` so Xcode still merges the existing scene/launch/orientation settings. Set `INFOPLIST_FILE = App/Resources/Info.plist` only on the application target, never the project or test targets. Do not add the plist or xcconfig files to Copy Bundle Resources.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>AppEnvironment</key><string>$(APP_ENVIRONMENT)</string>
    <key>APIBaseURL</key><string>$(API_BASE_URL)</string>
</dict>
</plist>
```

Clone each existing Debug/Release configuration into the six named configurations for the project, app, DesignSystemUITests, and AppTests. Preserve non-environment build settings and use new unique PBX object identifiers. Attach each composed xcconfig as the project configuration's `baseConfigurationReference`. Remove higher-precedence copies of keys now owned by Base/Debug/Release xcconfig files, including the former project-level Swift 5 language setting; preserve other analyzer settings. Keep scheme/build product names and module names stable.

Use these target-specific settings, which intentionally reference inherited environment values:

| Target | Settings |
| --- | --- |
| NativeTemplate | `PRODUCT_BUNDLE_IDENTIFIER = $(APP_ID)`; `INFOPLIST_KEY_CFBundleDisplayName = $(APP_DISPLAY_NAME)`; app-only `INFOPLIST_FILE` |
| DesignSystemUITests | `PRODUCT_BUNDLE_IDENTIFIER = $(APP_ID).DesignSystemUITests`; `TEST_TARGET_NAME = NativeTemplate`; generated test plist |
| AppTests | `PRODUCT_BUNDLE_IDENTIFIER = $(APP_ID).AppTests`; host/loader from step 1; generated test plist |

Set configuration-list defaults to Release-Prod; shared commands select Dev explicitly. Verify all target configuration names match and no Release configuration inherits DEBUG or the app-only plist path into test targets.

- [ ] **6. Compose the normal app startup and error state.** Store configuration once as `private let configuration = Result { try AppConfig.load() }` on NativeTemplateApp. Preserve the `#if DEBUG` test-host branch. Ordinary startup switches on that Result:

```swift
@ViewBuilder
private var configuredContent: some View {
    switch configuration {
    case .success(let config):
        ContentView(config: config,
                    request: ApiRequestFactory(baseURL: config.apiBaseURL).healthRequest())
    case .failure(let error):
        DSStatusView("Configuration error", description: error.localizedDescription, kind: .error)
            .padding(DSSpacing.lg)
    }
}
```

Import DesignSystem into NativeTemplateApp. Define `ContentView(config: AppConfig, request: URLRequest)` using a ScrollView/VStack with native fonts and `DSSpacing.lg`; display name, `Environment: <rawValue>`, `API base URL: <absoluteString>`, and `GET <request URL>`. Use the same semantic labels as Android. Preserve system background and wrapping at large Dynamic Type. The configuration error renders without constructing a request; rebuilding valid configuration is its recovery action.

- [ ] **7. Replace the shared scheme with three environment schemes.** Copy its app build entry and Debug test-host setup. Add AppTests beside DesignSystemUITests in TestAction, both with `skipped = NO`. Use the app's existing BlueprintIdentifier and the actual new AppTests target identifier. Set actions according to this table, then delete the old scheme:

| Scheme | Run / Test / Analyze | Profile / Archive |
| --- | --- | --- |
| NativeTemplate-Dev | Debug-Dev | Release-Dev |
| NativeTemplate-Stg | Debug-Stg | Release-Stg |
| NativeTemplate-Prod | Debug-Prod | Release-Prod |

Shared scheme XML must name the corresponding configuration in each `LaunchAction`, `TestAction`, `AnalyzeAction`, `ProfileAction`, and `ArchiveAction`. Test bundles are built for testing only. Preserve the existing app MacroExpansion reference so app-hosted tests load the right app.

- [ ] **8. Verify the resolved configurations and tests.** Run `xcodebuild -list -project apps/ios/NativeTemplate.xcodeproj`. For each row above, run Debug `build-for-testing` and Release `build`, and inspect the produced plist with `plutil -extract <key> raw -o - <plist>` for `CFBundleIdentifier`, `CFBundleDisplayName`, `AppEnvironment`, and `APIBaseURL`. Compare all four against the leaf file after resolving its `$()` escape. Inspect `-showBuildSettings` for each configuration to ensure Debug has DEBUG and Release does not. Add an AppTests check that loading Bundle.main returns a known environment with the proper ID suffix. Run AppTests for all three Debug schemes on the dedicated simulator.

- [ ] **9. Update native usage and commit the iOS deliverable.** Document scheme selection, native endpoint edits/rebuilds, explicit app plist, simulator paths, all app/test rename settings, and dedicated-simulator test commands. Update the iOS design-system README to use `NativeTemplate-Dev` / `Debug-Dev`, with all three schemes including its tests. Commit only task-owned files after the checks, for example `feat(ios): add native environment schemes and request example`.

## Task 3: Shared verification matrix and configuration parity

**Files:** `tooling/scripts/lib.sh`, `tooling/tests/test_commands.py`, and new `tooling/tests/test_environment_configuration.py`.

**Consumes:** Task 1 Gradle task/artifact names, Task 2 scheme/configuration/plist keys, and four-key native leaf settings.

**Produces:** Unchanged command syntax; `build` selects dev Debug; `verify` validates all six variants; all existing dispatch/failure/logging guarantees remain.

- [ ] **1. Extend fake tooling and add failing matrix tests.** The current FAKE_TOOL only recognizes one unflavoured Android build and one Debug iOS build. Update it to accept the following exact expected task sets, use variant-specific output paths, and write a real test plist with Python `plistlib`.

Android verify uses one Gradle invocation starting `--no-daemon`, followed by assemble/lint/test tasks for DevDebug, DevRelease, StgDebug, StgRelease, ProdDebug, and ProdRelease, then the existing library lint and component-test tasks once. Android build uses only `:app:assembleDevDebug`.

iOS verify uses six invocations in dev Debug/Release, stg Debug/Release, prod Debug/Release order. Debug action is `build-for-testing`; Release action is `build`. iOS build runs only NativeTemplate-Dev / Debug-Dev / build. Keep the generic simulator destination, `.build/ios` DerivedData path, and `CODE_SIGNING_ALLOWED=NO`.

Fake SDK setup includes an executable `build-tools/36.0.0/aapt2`; fake PATH includes `plutil`. Fake aapt2 prints the id/name stored inside a JSON fake APK, so it tests artifact metadata rather than echoing expected source settings. Fake plutil implements `-extract KEY raw -o - FILE` against the actual fake plist; missing/invalid keys return nonzero. Copy only the six native leaf config files into the fixture repository.

```python
def test_verify_dispatches_all_android_variants(self):
    self.assert_success(self.run_cli('verify', 'android'))
    calls = [call for call in self.calls() if call['tool'] == 'gradlew']
    self.assertEqual(len(calls), 1)
    expected = ['--no-daemon']
    for environment in ('Dev', 'Stg', 'Prod'):
        for mode in ('Debug', 'Release'):
            variant = environment + mode
            expected += [f':app:assemble{variant}', f':app:lint{variant}',
                         f':app:test{variant}UnitTest']
    expected += [':core:designsystem:lintDebug', ':core:designsystem:testDebugUnitTest']
    self.assertEqual(calls[0]['args'], expected)

def test_verify_dispatches_all_ios_variants(self):
    self.assert_success(self.run_cli('verify', 'ios'))
    calls = [call for call in self.calls()
             if call['tool'] == 'xcodebuild' and call['args'] != ['-version']]
    actual = [(call['args'][call['args'].index('-scheme') + 1],
               call['args'][call['args'].index('-configuration') + 1],
               call['args'][-1]) for call in calls]
    expected = [(f'NativeTemplate-{environment}', f'{mode}-{environment}',
                 'build-for-testing' if mode == 'Debug' else 'build')
                for environment in ('Dev', 'Stg', 'Prod')
                for mode in ('Debug', 'Release')]
    self.assertEqual(actual, expected)
```

Run the suite before changing the real dispatcher. Expect matrix/default-artifact assertions to fail because the real commands still select unflavoured Debug.

- [ ] **2. Add failure cases that target later artifacts and source/compiled mismatches.** Extend fake controls with `NATIVE_FAKE_FAIL_VARIANT`, `NATIVE_FAKE_MISSING_VARIANT`, `NATIVE_FAKE_BAD_ID_VARIANT`, `NATIVE_FAKE_BAD_NAME_VARIANT`, and `NATIVE_FAKE_BAD_URL_VARIANT`. Use `stgRelease` / `Release-Stg` values to isolate a later build. Preserve existing native-status controls and add an app-test failure status distinct from library-test failure.

```python
def test_missing_staging_release_artifact_does_not_pass(self):
    for platform, variant in [('android', 'stgRelease'), ('ios', 'Release-Stg')]:
        with self.subTest(platform=platform):
            self.assert_success(self.run_cli('build', platform))
            result = self.run_cli('verify', platform, NATIVE_FAKE_MISSING_VARIANT=variant)
            self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
            self.assertIn('artifact', result.stdout + result.stderr)

def test_wrong_ios_endpoint_does_not_pass(self):
    result = self.run_cli('verify', 'ios', NATIVE_FAKE_BAD_URL_VARIANT='Release-Stg')
    self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
    self.assertIn('APIBaseURL', result.stdout + result.stderr)
```

Also assert ID/name mismatches fail on both platforms, a later native failure retains its exact exit status despite earlier artifacts, and the aggregate command still attempts iOS after Android fails. Preserve all existing invalid-usage, paths-with-spaces, prerequisite isolation, concurrent-output, library-test, and iOS-test-compilation cases. Update their expected default artifact paths without removing their assertions.

- [ ] **3. Add small native-settings readers and artifact validation helpers.** Keep these internal POSIX functions in lib.sh:

```sh
native_setting() (
    native_settings_file=$1
    native_settings_key=$2
    [ -f "$native_settings_file" ] || exit 1
    case "$native_settings_file" in
        *.xcconfig) native_decode_xcconfig=1 ;;
        *) native_decode_xcconfig=0 ;;
    esac
    awk -v wanted="$native_settings_key" -v decode="$native_decode_xcconfig" '
        /^[[:space:]]*(#|\/\/)/ { next }
        index($0, "=") {
            name = substr($0, 1, index($0, "=") - 1)
            value = substr($0, index($0, "=") + 1)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
            if (name == wanted) {
                if (decode) gsub(/\$\(\)/, "", value)
                print value
                found = 1
                exit
            }
        }
        END { if (!found) exit 1 }
    ' "$native_settings_file"
)
```

This reads only the documented four-key leaf format, not arbitrary xcconfig includes, Java-properties escapes, or shell syntax. Do not source files as shell code. Native build tools remain authoritative for compilation; this reader compares intended leaf values to artifacts. Add fixture coverage for blank/missing keys and paths containing spaces. Native config files must reject/include no duplicate keys; the parity test catches duplicates rather than choosing one silently.

Define `native_android_artifact <env> <mode>` to check the exact nonempty APK path, invoke `"$ANDROID_HOME/build-tools/36.0.0/aapt2" dump badging "$native_artifact"`, and require the expected package ID and default application label from that environment's `.properties`. Reject a missing/nonexecutable aapt2 in Android doctor. On failed metadata extraction, return 1 with the artifact/key in the diagnostic. APK name/identity checks supplement the compiled endpoint tests; do not claim badging proves BuildConfig endpoint values.

Define `native_ios_artifact <EnvironmentTitle> <Mode>` to check the executable and plist under `<Mode>-<EnvironmentTitle>-iphonesimulator`. Compare each of these pairs using `plutil -extract "$native_plist_key" raw -o - "$native_plist"` and `native_setting`:

| Plist key | Native leaf key |
| --- | --- |
| CFBundleIdentifier | APP_ID |
| CFBundleDisplayName | APP_DISPLAY_NAME |
| AppEnvironment | APP_ENVIRONMENT |
| APIBaseURL | API_BASE_URL |

Reject unresolved `$(` expressions and blank/missing metadata. Return 1 with the configuration and key on a mismatch. Require plutil only for iOS, preserving platform prerequisite independence.

- [ ] **4. Change native_compile to select and check the matrix.** Android assembles task arguments without eval or parsing Gradle task names from user input:

```sh
set -- --no-daemon
if [ "$native_action" = verify ]; then
    for native_environment in Dev Stg Prod; do
        for native_mode in Debug Release; do
            native_variant="$native_environment$native_mode"
            set -- "$@" ":app:assemble$native_variant" ":app:lint$native_variant" ":app:test${native_variant}UnitTest"
        done
    done
    set -- "$@" :core:designsystem:lintDebug :core:designsystem:testDebugUnitTest
else
    set -- "$@" :app:assembleDevDebug
fi
./gradlew "$@" || exit $?
```

After successful Gradle execution, call `native_android_artifact` for dev/debug only on build and all six combinations on verify. Stop on the first artifact failure and preserve Gradle's status on native failure.

For iOS choose `native_environments='Dev'` / `native_modes='Debug'` on build and `native_environments='Dev Stg Prod'` / `native_modes='Debug Release'` on verify. Loop over these fixed lists. Choose build-for-testing only when verifying Debug. Use the existing xcodebuild argument structure with `-scheme "NativeTemplate-$native_environment" -configuration "$native_mode-$native_environment"`, then call `native_ios_artifact` immediately after each successful invocation. Propagate the first native/artifact failure within the platform. Leave native_dispatch's aggregate behavior and per-platform logging intact.

- [ ] **5. Add repository configuration parity and scheme-contract tests.** The new test module uses only pathlib, unittest, xml.etree.ElementTree, and tempfile for malformed-input fixtures. Define `REPO = Path(__file__).resolve().parents[2]` and this private leaf reader. Decode `$()` only in xcconfig files; an Android literal must not be changed by the verification parser.

```python
def read_leaf(path):
    required = {'APP_ENVIRONMENT', 'APP_ID', 'APP_DISPLAY_NAME', 'API_BASE_URL'}
    values = {}
    for raw in path.read_text(encoding='utf-8').splitlines():
        line = raw.strip()
        if not line or line.startswith(('#', '//')):
            continue
        key, separator, value = line.partition('=')
        key, value = key.strip(), value.strip()
        if not separator or key not in required or key in values or not value:
            raise ValueError(f'{path}: invalid or duplicate key {key}')
        values[key] = value.replace('$()', '') if path.suffix == '.xcconfig' else value
    if set(values) != required:
        raise ValueError(f'{path}: missing keys {sorted(required - set(values))}')
    return values
```

Test each environment's Android/iOS dictionaries for equality, three distinct identities, canonical names, suffixes, and the unsuffixed production identity.

```python
def test_native_leaf_configuration_agrees(self):
    for environment, title in [('dev', 'Dev'), ('stg', 'Stg'), ('prod', 'Prod')]:
        with self.subTest(environment=environment):
            android = read_leaf(REPO / f'apps/android/config/{environment}.properties')
            ios = read_leaf(REPO / f'apps/ios/Config/{title}.xcconfig')
            self.assertEqual(android, ios)
            self.assertEqual(android['APP_ENVIRONMENT'], environment)
            suffix = '' if environment == 'prod' else '.' + environment
            self.assertEqual(android['APP_ID'], 'com.example.nativetemplate' + suffix)
```

Parse each shared scheme and assert its Run/Test/Analyze Debug mapping, Profile/Archive Release mapping, AppTests and DesignSystemUITests entries, and app MacroExpansion reference. These tests protect executable build interfaces, not file formatting. Use small temporary fixtures to prove duplicate keys and mismatched endpoint values fail. A shared endpoint edit is made in both native leaf files, not in a third table of hardcoded endpoints in the tests.

- [ ] **6. Run the entire command suite and native verifications, then commit.** Run `python3 -m unittest discover -s tooling/tests -v`, `./tooling/scripts/doctor android`, `./tooling/scripts/doctor ios`, `./tooling/scripts/verify android`, and `./tooling/scripts/verify ios`. Inspect every exit status and report test compilation separately from execution. Do not broaden tests repeatedly after passing without a new change or unresolved concern. Commit the checked shared tooling deliverable as `feat(tooling): verify all native environments`.

## Task 4: Runtime evidence, documentation, and final review

**Files:** Current guides and decision/evidence files in the file map. Native sources change only if a measured failure needs a fix, followed by the relevant test/build rerun.

**Consumes:** All six native artifacts per platform, passing shared suite, native app tests, and unchanged component suites.

**Produces:** Evidence for each acceptance scenario, explicit limitations, current usage documentation, and a reviewable local feature change. No publishing, store submission, CI provider integration, or release signing is included.

- [ ] **1. Run iOS tests on a dedicated simulator.** Discover available runtimes/devices with `xcrun simctl list runtimes -j` and `xcrun simctl list devicetypes -j`. Create a new test-owned iPhone simulator from an installed compatible pair; assign its UUID to `NATIVE_ENV_TEST_DEVICE`. Never select `booted` or delete an existing device. Boot and wait for readiness. For each environment, run AppTests and the existing component UI suite:

```sh
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate-Dev -configuration Debug-Dev \
  -destination "platform=iOS Simulator,id=$NATIVE_ENV_TEST_DEVICE" \
  -derivedDataPath .build/ios-environment-tests/dev \
  -resultBundlePath .build/ios-environment-tests/dev-results.xcresult \
  -parallel-testing-enabled NO -collect-test-diagnostics never \
  -only-testing:AppTests -only-testing:DesignSystemUITests \
  CODE_SIGN_IDENTITY=- test
```

Use corresponding Stg/Prod configuration and unique result/output paths for the other two runs. Handle an existing result directory by choosing a fresh run-specific path, not deleting someone else's evidence. These signed simulator test builds are separate from unsigned generic-simulator verification.

- [ ] **2. Install and launch all three iOS identities together.** Install each Debug app from its dedicated test build to the same dedicated simulator. Launch by the exact bundle IDs in the contract, without `--design-system-tests`. Use simctl listapps to confirm all three remain installed, and capture each screen with `xcrun simctl io "$NATIVE_ENV_TEST_DEVICE" screenshot <task-owned-output.png>`. Inspect screenshots for the name, environment, full endpoint, and matching health request. Repeat a screen inspection with enlarged text and a longer endpoint in a disposable checkout if needed to exercise wrapping. Record only observed UI evidence. Shut down/delete only the simulator created for this task after retaining results and screenshots.

- [ ] **3. Inspect all three Android identities on a dedicated runtime.** Discover installed emulator images/AVDs and adb devices. Use or create only a task-owned disposable emulator; do not select a user's device implicitly. Set `NATIVE_ENV_ANDROID_DEVICE` to its serial, install all three Debug APKs with `adb -s "$NATIVE_ENV_ANDROID_DEVICE" install -r <apk>`, and launch each with an explicit component:

```sh
adb -s "$NATIVE_ENV_ANDROID_DEVICE" shell am start -W \
  -n com.example.nativetemplate.dev/com.example.nativetemplate.MainActivity
```

Use the staging/production application IDs with the same Kotlin activity class. Confirm package coexistence and inspect screen captures for each environment. With larger font scale on this dedicated emulator, check full URL wrapping/scrolling. Preserve the prior setting for cleanup. If no usable image/runtime is installed, report Android launch evidence as unverified and continue unaffected checks; a build pass does not replace it.

- [ ] **4. Prove configuration edits and independent native builds.** In a disposable checkout/copy, change only the dev endpoint on each platform to `https://dev-api.example.com/v2/`, build the dev app, and verify the constructed request is `https://dev-api.example.com/v2/health` using the native tests and runtime example. Restore/discard only task-owned scratch changes. Run direct Gradle from `apps/android` and direct xcodebuild from `apps/ios` to prove no root generator is required. IDE import/build evidence is recorded only if actually observed through an authorized IDE session; command-line builds alone leave it unverified.

- [ ] **5. Complete required build-isolation checks.** Follow `docs/workflows/verification.md`: native clean, Android then iOS then Android; one Android and one iOS build concurrently from a directory outside the repo using absolute script paths; confirm artifact identities, separate logs/statuses, and unchanged tracked input hashes. Use a task-specific `NATIVE_ENV_REPO` variable for the checkout path. Preserve stdout and exit statuses separately; do not label aggregate success after one background command failed. Run at most one build per platform at a time. Native caches and generated outputs remain ignored.

- [ ] **6. Write the architecture decision and current usage documentation.** Use `tooling/templates/decision.md` for `0002-native-environment-configuration.md`: accepted native files, build-time selection, app-owned config/request types, independent builds, duplicated public values checked for parity, and rejected `.env`/runtime-switching alternatives. Link the approved spec and evidence record. Update current guides to the exact new commands and artifact paths; include this everyday example:

```sh
# From repository root: default dev Debug builds.
./tooling/scripts/build android
./tooling/scripts/build ios

# From apps/android: selected staging Release build (unsigned).
./gradlew --no-daemon :app:assembleStgRelease

# From repository root: selected staging Release simulator build.
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate-Stg -configuration Release-Stg \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/ios CODE_SIGNING_ALLOWED=NO build
```

Explain where to edit both platform endpoints, how to rename all identities, why Release can target staging, why the demonstration does not send a request, and that `verify ios` compiles tests but the dedicated simulator command executes them. Update the architecture/design-system descriptions of the former neutral screen and mention the app unit-test target. Keep historical evidence immutable.

- [ ] **7. Record measured results and perform one final independent review.** Structure the evidence document using the work-item template: objective and scope; platform/tool versions; command/exit/result table; test counts; artifact locations; runtime/IDE evidence; isolation results; limitations and next actions. A missing tool is not a pass. Avoid personal paths, credentials, and simulator UUIDs in tracked prose. Use repo-relative ignored result paths.

Review the final diff against all nine spec acceptance scenarios, the five review-focus items, native boundaries, and existing instruction files. Run `git diff --check` and verify Git status before committing documentation. Run the requesting-code-review workflow for one independent final review if executing natively; fix actionable findings and rerun only affected checks. Do not claim acceptance scenarios with missing runtime or IDE evidence are verified. Commit the final documentation/evidence as `docs: record native environment workflow and verification`.

## Spec coverage and plan review

| Spec requirement | Owning tasks / evidence |
| --- | --- |
| Independent native builds and checked-in configuration | Tasks 1–2; Task 4 direct builds/isolation |
| Three installable identities and display names | Tasks 1–2; Task 3 metadata checks; Task 4 simultaneous installation |
| Dummy endpoint → typed config → request example | Tasks 1–2 unit tests and app wiring; Task 4 launch inspection |
| Endpoint edits require only configuration + rebuild | Tasks 1–2 adapters; Task 4 disposable edit probe |
| Environment distinct from Debug/Release | Six-variant tests/builds in Tasks 1–3; scheme tests |
| Explicit invalid configuration behavior | Android Gradle negative probe and model tests; iOS error/result and tests |
| Paths/trailing slash/no external service | Request tables in Tasks 1–2 |
| Existing design-system test-host behavior | Task 2 conditional compilation; Task 4 XCTest on all schemes |
| Command guarantees, parity, and artifacts | Task 3 fake-tool suite and native checks |
| Documentation, decision, evidence, limitations | Task 4 |

The plan adds Release app unit-test execution to the spec's minimum Debug tests to verify the compiled environment values in every Android variant. It adds no new product behavior, external service, or platform dependency.

## Planning evidence and native references

During planning, both prerequisite checks reported the installed Android JBR 25.0.3/SDK 36 toolchain and Xcode 27.0 simulator SDK. Implementation tests, build matrices, IDE imports, and runtime checks have not run for this feature.

- [Android flavour/build variant behavior](https://developer.android.com/build/build-variants) and [AGP 9.2 variant fields/resources](https://developer.android.com/reference/tools/gradle-api/9.2/com/android/build/api/dsl/VariantDimension) support the proposed native Gradle wiring.
- [Apple xcconfig syntax/precedence](https://developer.apple.com/documentation/xcode/adding-a-build-configuration-file-to-your-project) informs include composition and URL escaping.
- [Apple plist processing](https://developer.apple.com/documentation/bundleresources/managing-your-app-s-information-property-list) explains why custom environment keys belong in the explicit input plist and why it must not be copied as a resource.
- [Apple build settings](https://developer.apple.com/documentation/xcode/build-settings-reference) documents host, bundle, and plist configuration.
- [AAPT2 metadata inspection](https://developer.android.com/tools/aapt2) supports APK identity/name validation.

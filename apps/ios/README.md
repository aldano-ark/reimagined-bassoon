# iOS

An independent Swift/SwiftUI application targeting iOS 17 and later. Open `NativeTemplate.xcodeproj` in Xcode. The project and `NativeTemplate` scheme are checked in; no project generator, Ruby, Android SDK, or Gradle is required.

## Prerequisites

Install Xcode with the iOS SDK and complete its normal first-launch setup. This template is verified with Xcode 27.0 and Swift 6.4 in Swift 6 language mode; other Xcode versions have not been verified.

Use command-scoped `DEVELOPER_DIR` if selecting a different installed Xcode. Simulator builds need no development team or distribution credentials. Device and store signing are configured when a product needs them.

## Environments and builds

Select `NativeTemplate-Dev`, `NativeTemplate-Stg`, or `NativeTemplate-Prod` in Xcode. Each scheme uses its matching Debug configuration for Run/Test/Analyze and Release configuration for Profile/Archive. Environment and build mode are independent.

| Environment | Bundle identifier | Display name |
| --- | --- | --- |
| dev | com.example.nativetemplate.dev | NativeTemplate Dev |
| stg | com.example.nativetemplate.stg | NativeTemplate Stg |
| prod | com.example.nativetemplate | NativeTemplate |

Public values live in `Config/Dev.xcconfig`, `Stg.xcconfig`, and `Prod.xcconfig`. Edit `APP_ENVIRONMENT`, `APP_ID`, `APP_DISPLAY_NAME`, and `API_BASE_URL` there, then rebuild. Keep one literal assignment per line with no inline comments or interpolation other than the URL escape described below. The corresponding Android values must agree; the shared Python suite checks parity without participating in Xcode compilation.

Use `https:/$()/dev-api.example.com` in xcconfig source: `$()` expands to empty, preserving `https://` without starting a comment. The checked-in `App/Resources/Info.plist` expands the custom environment and URL keys. The app validates its resolved configuration once and displays an explicit configuration error if required values are missing or invalid. There is no fallback endpoint.

From the repository root:

```sh
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate-Dev -configuration Debug-Dev \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/ios CODE_SIGNING_ALLOWED=NO build

xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate-Stg -configuration Release-Stg \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/ios CODE_SIGNING_ALLOWED=NO build
```

`./tooling/scripts/build ios` selects dev Debug. `./tooling/scripts/verify ios` compiles tests for all three Debug configurations and builds all three Release configurations, checking each app's executable, identity, display name, environment, and full API URL.

Applications are under `.build/ios/Build/Products/<Configuration>-iphonesimulator/NativeTemplate.app`, for example `Debug-Dev-iphonesimulator`. Generic simulator verification does not establish device signing or distribution readiness.

The template screen receives an `AppConfig` and a health URLRequest constructed by `ApiRequestFactory`. It displays the selected environment, dummy endpoint, and `GET /health` request. It does not send a request. A configured base path such as `/v2/` is preserved in `/v2/health`. No networking library, `.env` file, or configuration-generation command is required.

Configuration values are embedded in the app. Keep credentials and signing material outside these files.

## Execute tests

The shared verification command compiles AppTests and DesignSystemUITests; it does not execute them. Set `NATIVE_ENV_TEST_DEVICE` to a dedicated test simulator UUID, then run:

```sh
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate-Dev -configuration Debug-Dev \
  -destination "platform=iOS Simulator,id=$NATIVE_ENV_TEST_DEVICE" \
  -derivedDataPath .build/ios-environment-tests/dev \
  -parallel-testing-enabled NO \
  -only-testing:AppTests -only-testing:DesignSystemUITests \
  CODE_SIGN_IDENTITY=- test
```

Use matching Stg/Prod schemes and configurations with separate output paths for those environments. AppTests cover validation, packaged identity, and request construction. The [DesignSystem guide](Packages/Core/DesignSystem/README.md) explains the Debug-only component fixture. Keep existing personal simulator sessions separate from automated test runs.

## Rename for a product

1. Change `APP_ID` and `APP_DISPLAY_NAME` in all three environment xcconfig files. Change the app product name in its target settings. Update the identity contract in app/shared tests.
2. Rename the project, target, and all three shared schemes through Xcode when changing `NativeTemplate`; keep the schemes shared.
3. Rename `NativeTemplateApp` and its file, updating project references, and change the neutral screen title in `ContentView.swift`.
4. Add a product app icon to the asset catalog and select it in the target settings.
5. Update the project/scheme and artifact references in shared tooling and command tests after changing target/product names. Update the Android name/identifier independently.
6. Update `DesignSystemUITests.TEST_TARGET_NAME`, `AppTests.TEST_HOST`, and `AppTests.BUNDLE_LOADER` when renaming the app product. Test bundle identifiers derive from `APP_ID`; keep both test targets registered in all three schemes.

See [feature packages](Packages/Features/README.md) and [core packages](Packages/Core/README.md) before adding packages.

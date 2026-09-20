# iOS

An independent Swift/SwiftUI application targeting iOS 17 and later. Open `NativeTemplate.xcodeproj` in Xcode. The project and `NativeTemplate` scheme are checked in; no project generator, Ruby, Android SDK, or Gradle is required.

## Prerequisites

Install Xcode with the iOS SDK and complete its normal first-launch setup. This template is verified with Xcode 27.0 and Swift 6.4 in Swift 6 language mode; other Xcode versions have not been verified.

Use command-scoped `DEVELOPER_DIR` if selecting a different installed Xcode. Simulator builds need no development team or distribution credentials. Device and store signing are configured when a product needs them.

## Build

From the repository root:

```sh
xcodebuild -project apps/ios/NativeTemplate.xcodeproj \
  -scheme NativeTemplate -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/ios CODE_SIGNING_ALLOWED=NO build
```

Shared entry points: `./tooling/scripts/build ios` and `./tooling/scripts/verify ios`. Both compile the app and validate its executable and `Info.plist`. They do not run simulator tests.

Application: `.build/ios/Build/Products/Debug-iphonesimulator/NativeTemplate.app`.

## Rename for a product

1. Change the bundle identifier, product name, and display name in the app target's build settings.
2. Rename the project, target, and shared scheme through Xcode when changing `NativeTemplate`; keep the scheme shared.
3. Rename `NativeTemplateApp` and its file, updating project references, and change the neutral screen title in `ContentView.swift`.
4. Add a product app icon to the asset catalog and select it in the target settings.
5. Update the project/scheme and artifact references in shared tooling and command tests after changing target/product names. Update the Android name/identifier independently.

See [feature packages](Packages/Features/README.md) and [core packages](Packages/Core/README.md) before adding packages.

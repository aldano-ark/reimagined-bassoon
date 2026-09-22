# Pokémon Collection — Android

A separate offline Kotlin/Compose example for Android 8.0+ (API 26), targeting SDK 36. Open this directory in Android Studio. The template under `apps/android/` remains unchanged.

## Run and verify

Use the repository's installed Android SDK/JDK prerequisites, with `ANDROID_HOME` and `JAVA_HOME` supplied by your environment. No SDK paths are checked in.

```sh
./gradlew :app:assembleDebug
./scripts/verify --console=plain
```

The verification entry point runs the feature's domain/persistence/Compose tests, the app's Activity recreation test, Debug/Release lint, and Debug/Release APK builds. Tests run through Robolectric and require no attached device. The first run may download Gradle, Android dependencies, and the Robolectric Android runtime.

Select an emulator in Android Studio and run `app`, or explicitly target an emulator serial:

```sh
adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5580 shell am start -n com.example.pokemoncollection/.MainActivity
```

Replace the sample serial with your own emulator's serial. The Release APK is unsigned; signing/distribution is outside this example.

## Boundaries and behavior

- `app/` assembles a retained `CollectionModel`, feature store/controller, and root Collection/Wishlist tabs. In-flight writes and the committed state survive Activity recreation.
- `features/collection/` owns catalog search, galleries, details, copy entry validation, wishlist, atomic persistence, screen state, and tests. Copies and wishlist start empty.
- `:core:designsystem` references the existing read-only template library. Its build output goes into this example's `build/designsystem/`.
- The feature's variant asset source packages `../assets/` directly. No Android build step invokes or reads the iOS project.
- User data lives in the app-private `pokemon-collection-v1.json` file under application ID `com.example.pokemoncollection`. The schema has a version, individually identified/timestamped copy entries, and timestamped wishlist memberships.
- A successful save means atomic file replacement succeeded. Failed writes retain the committed state and expose Retry. Read failures block mutation and preserve the file. Unsupported versions, invalid entries, duplicate IDs, trailing corruption, and invalid UTF-8 are rejected. There is no reset/delete-all UI.
- Finishes come from the bundled catalog. Quantity accepts 1–99; note validation and the visible counter share a limit of 500 user-visible Unicode characters. Editing a failed add draft clears its stale retry action.

The UI uses `DSTheme`, `DSButton`, `DSTextField`, `DSStatusView`, and `DSSpacing`, with native Material controls for tabs, chips, menus, and confirmation dialogs. The feature provides sage light/dark palettes, complete card images, two gallery columns at ordinary text size, one column for enlarged text, and scrolling content on compact displays. Debug-only previews cover empty, populated, dark wishlist, and large-text states.

See [VERIFICATION.md](VERIFICATION.md) for executed checks and limits.

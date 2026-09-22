# Pokémon Collection for iOS

Open `PokemonCollection.xcodeproj` and select the shared **PokemonCollection** scheme. This independent SwiftUI app supports iOS 17+ and uses Swift 6. The checked-in project builds without a generator or package installation. Xcode 27.0 is the verified toolchain.

The app starts with an empty collection and wishlist. Use **Find your first card** or **+** to search the bundled seven-card example catalog. Open any card to add a copy entry or change wishlist membership. Entries retain quantity, available finish, condition, optional note, and save date. Confirm removal to delete precisely that entry. Owning a card and wanting a card are independent states.

`App/` assembles file storage, the bundled catalog, the feature, and native tab/navigation containers. `Packages/CollectionFeature` owns presentation, search, validation, and saved state. It depends on the existing `apps/ios/Packages/Core/DesignSystem` package without changing it. Xcode copies `../assets` as a folder resource. Neither compilation nor runtime depends on Android or root scripts.

The collection is an atomic JSON document under this app's Application Support directory, isolated by bundle ID `com.example.pokemoncollection`. Mutations publish only after a successful write. Corrupt, unsupported, or unreadable documents show **Retry** and block changes; the app never resets them automatically. A failed save leaves the form and committed collection intact for retry. There is no network or cloud service.

## Build and test

From the repository root:

```sh
examples/pokemon-collection/ios/scripts/verify
python3 -m unittest discover -s examples/pokemon-collection/ios/scripts/tests -v
```

The first command uses Python 3.9+ and Xcode to compile Debug and its tests, build Release, and execute unit/UI tests. It chooses an installed compatible iOS runtime, creates its own uniquely named iPhone simulator, and deletes that simulator afterward. It never selects or resets existing devices. Logs, result bundles, screenshot attachments, and cleanup status remain in `ios/.build/verification-*`. The shared `ios/.build/DerivedData` means only one invocation should run at a time.

Each native command runs in its own process group. Interruptions/timeouts stop and reap that group before simulator cleanup. Boot has a five-minute timeout; builds/tests have twenty minutes each; other commands have one minute. SIGKILL or host shutdown cannot run cleanup; the retained summary/logs identify the owned simulator if manual deletion is needed.

Use `scripts/verify --unit-only` from this directory for a shorter state/persistence test run, or `scripts/verify --ui-only` for focused interaction and screenshot verification. `DEVELOPER_DIR` can be set per command without changing the system Xcode selection.

Direct build commands from this directory:

```sh
xcodebuild -project PokemonCollection.xcodeproj -scheme PokemonCollection \
  -configuration Debug -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/DerivedData CODE_SIGNING_ALLOWED=NO build-for-testing

xcodebuild -project PokemonCollection.xcodeproj -scheme PokemonCollection \
  -configuration Release -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/DerivedData CODE_SIGNING_ALLOWED=NO build
```

For interactive use, select a simulator in Xcode and Run. Device signing requires your own configuration. The feature includes debug-only empty collection/wishlist previews. UI tests use unique debug-only storage filenames and an explicit dark-appearance launch flag; normal launches use the personal collection file, and Release ignores those flags.

See [VERIFICATION.md](VERIFICATION.md) for measured behavior, screenshots, and limitations.

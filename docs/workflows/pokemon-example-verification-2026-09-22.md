# Pokémon example verification — 2026-09-22

## Scope and isolation

Branch: `codex/pokemon-collection-example`, based on merged PR #2 (`6f0ab59`). New native projects, feature code, resources, and commands live under `examples/pokemon-collection/`. Template apps, shared tooling, template CI, and website are outside the implementation scope.

## Baseline

- `./tooling/scripts/doctor all`: passed; JDK 25.0.3, Android platform/build tools 36, Xcode 27.0 (27A266a), iOS Simulator SDK 27.0.
- `./tooling/scripts/verify all`: passed before example builds. Android's six app variants and library checks passed; iOS Debug test compilation and Release builds passed for dev/stg/prod. Log: `.build/pokemon-template-baseline.log` (ignored).
- `python3 -m unittest discover -s tooling/tests -v`: 47 tests passed. Log: `.build/pokemon-tooling-baseline.log` (ignored).
- `./examples/pokemon-collection/scripts/verify assets`: 4 tests passed, checking stable catalog identity, real image resources/dimensions, supported finishes, and separation of catalog from user data.

## Native examples

| Check | Result |
| --- | --- |
| `./examples/pokemon-collection/scripts/verify android` | Passed: 11 state/persistence tests, 6 Compose tests, 1 Activity recreation test; zero failures/errors/skips. App and feature Debug/Release lint passed with zero errors; both APKs built. |
| `./examples/pokemon-collection/scripts/verify ios` | Debug build-for-testing and Release build passed; 10 unit tests and 2 XCTest UI tests passed on iPhone 18 Pro / iOS 27.0. Dedicated simulator deletion confirmed in the run summary. |
| `python3 -m unittest discover -s examples/pokemon-collection/ios/scripts/tests -v` | 3 subprocess lifecycle tests passed: status propagation, timeout cleanup, signal/descendant cleanup. |
| Native resource inspection | Android APK and iOS Release bundle contain all seven metadata records and corresponding PNGs. iOS resolved app identity is `com.example.pokemoncollection`. |
| Example command arguments | Missing, unknown, and extra arguments return usage with exit 2. Shell syntax check passed. |

Detailed commands, red/green evidence, and native reports are in [Android verification](../../examples/pokemon-collection/android/VERIFICATION.md) and [iOS verification](../../examples/pokemon-collection/ios/VERIFICATION.md). The iOS full-run result is under `ios/.build/verification-20260922-191910-20dcb3/` (ignored). A focused final UI run, `verification-20260922-194923-0acb07`, passed both interaction tests with no runtime warnings and confirmed simulator deletion after the keyboard-toolbar correction.

## Runtime and visual observations

- On a dedicated API 36 Pixel 9 emulator, started empty, found Pikachu, selected Reverse holo, entered a note with the keyboard open, and saved. The save action remained visible above the keyboard. Details showed the saved finish, condition, note, and quantity.
- Saved the same card to Wishlist, force-stopped/relaunched the process, and verified collection and wishlist membership persisted independently. Installed a newer example APK over the earlier build without losing these entries, then added Bulbasaur through the UI. Counts reflected two unique cards and two copies.
- Reviewed full card artwork, normal collection layout, tabbed-content spacing, confirmation, form, and catalog. Fixed singular/plural labels and the remaining default purple control colors across related states.
- Verified the corrected Android catalog at a stable 2424×1080 landscape viewport with 200% text and dark appearance: headers scroll away, card identity/status rows remain reachable, and a card opens its details. The dedicated emulator was shut down after inspection.
- iOS XCTest exercised catalog search, add/cancel, finish/condition/note input, wishlist, relaunch, confirmation, and deletion. It opened every bundled catalog card and executed Add/Save with accessibilityXXXL text and dark appearance. Screenshots of normal collection/catalog and large-text/dark forms were inspected. Primary action fill was adjusted within the example to preserve white-label contrast in dark mode.
- Selected screenshots are committed in [the example guide](../../examples/pokemon-collection/README.md) and [the Android large-text view](../../examples/pokemon-collection/docs/screenshots/android-large-dark.png). Remaining screenshots/results are ignored native verification artifacts.

## Review and regression fixes

A separate read-only review identified an Android Activity recreation race: an old asynchronous write could finish after a new store had read stale data. The app now retains its store and mutation controller in a ViewModel. The executed regression recreates an Activity during a blocked write, then performs another mutation and reopens storage to verify both commits survive. A companion Compose test prevents a completed save from leaving a duplicate draft after recreation.

Additional regressions cover rejected corrupt/unsupported data, invalid UTF-8/trailing JSON, bounds validation, 500/501 grapheme notes including emoji and combining characters, and editing a failed draft before retry. Both platforms retain committed state on failed writes. Pricing, scanning, networking, and cloud sync remain explicitly outside this offline example.

The initial iOS full run reported two invalid-frame warnings when activating the note field's additional keyboard toolbar. Removing that redundant toolbar retained the native Done submit key and eliminated the warnings in the focused rerun of both UI tests, including note editing, relaunch/deletion, and accessibilityXXXL dark Add/Save. Release was rebuilt successfully after that correction.

## Limits

No files under `apps/`, `tooling/`, `.github/`, or `site/` differ from the branch baseline. Existing root CI still verifies the template; the example has separate verification commands. Generated output, SDK paths, logs, simulators, APKs, and result bundles are not tracked.

Minimum-supported OS/device execution, physical-device signing/install, VoiceOver/TalkBack traversal, and interactive IDE import were not verified. Android lint retains warnings for pinned versions, absent custom launcher artwork, and backup-rule metadata; no lint errors. The example catalog is seven cards, and data remains local to each app.

# iOS verification — 2026-09-22

## Scope and implementation

This example adds `App/`, the checked-in `PokemonCollection.xcodeproj` and shared scheme, `Packages/CollectionFeature`, `CollectionTests`, `CollectionUITests`, and the example-local verification runner. It consumes the existing DesignSystem package read-only and copies `../assets` into its bundle. No template code, template component, root tooling, or Android code is changed by this implementation.

Collection and wishlist start empty, have separate saved membership, and show computed totals. Search trims whitespace and matches case-insensitive names/sets and numeric printed numbers (`25` matches `025`). Sorting by recent save, name, or number does not modify the document. Each card opens details. Copy entries validate quantity 1–99, catalog-supported finish, condition, and a 500-visible-character note limit. Successful saves have visible confirmation; cancellations do not mutate state; deletion requires confirmation and removes one entry's quantity. Atomic file replacement and candidate-state validation preserve the committed collection on failed writes. Unreadable, corrupt, invalid, and unsupported-version documents block writes and present Retry.

The UI uses native NavigationStack, TabView, Form, Stepper, Picker, alerts, and the unchanged DSButton/DSTextField/DSStatusView/DSSpacing APIs. Normal phone text uses two gallery columns; accessibility text uses one. Card artwork is fully fitted. Named sage/surface assets support both appearances. Dark primary buttons retain a darker sage fill for native white-label contrast, while text and status accents use the lighter dark variant.

## Commands and results

Toolchain: Xcode 27.0 (27A266a), Apple Swift 6.4, iOS 27.0 simulator on a newly created iPhone 18 Pro. Deployment minimum is iOS 17.0.

From the repository root:

```sh
examples/pokemon-collection/ios/scripts/verify
examples/pokemon-collection/ios/scripts/verify --unit-only
examples/pokemon-collection/ios/scripts/verify --ui-only
python3 -m unittest discover -s examples/pokemon-collection/ios/scripts/tests -v
```

- Test-first state baseline: `--unit-only` against the implementation scaffold failed all six original cases, with 33 expected assertions for missing search, mutation, validation, and recovery behavior. Evidence: `.build/verification-20260922-190125-4027c3/`.
- Complete native flow: Debug build-for-testing, Release build, and ten state tests plus two UI tests passed, with ten screenshots exported. Evidence: `.build/verification-20260922-191910-20dcb3/`. The result bundle reported two invalid-frame runtime warnings; the investigation is recorded below.
- Expanded unit suite: all ten tests passed, including Unicode note boundaries and independently ordered saved-card sorts. Evidence: `.build/verification-20260922-191728-a832c0/`.
- Focused UI verification after the keyboard-toolbar correction: both UI tests passed with zero runtime warnings, ten exported screenshots, and successful simulator deletion. Evidence: `.build/verification-20260922-194923-0acb07/`.
- Final incremental Release build after the toolbar correction: `xcodebuild -project examples/pokemon-collection/ios/PokemonCollection.xcodeproj -scheme PokemonCollection -configuration Release -destination 'generic/platform=iOS Simulator' -derivedDataPath examples/pokemon-collection/ios/.build/DerivedData CODE_SIGNING_ALLOWED=NO build` passed. Log: `.build/verification-20260922-194923-0acb07/release-build.log`.
- Runner lifecycle: all three Python tests passed. Real child-process tests verify timeout/signal termination of descendants before return, cleanup after interruption, captured output, and nonzero exit propagation.

The native runner retains `debug-build.log`, `release-build.log`, `tests.log`, `Tests.xcresult`, exported `screenshots/manifest.json`, and `summary.json`. The summary records executed test counts and successful deletion of the run's owned simulator. Earlier failing configuration/selector checks were corrected before the passing runs. Xcode emitted its benign App Intents metadata-extraction notice; no Swift compilation errors remained.

## Behavior coverage

The ten unit tests cover empty state; whitespace/case/numeric search; separate wishlist/ownership; real atomic-file reopen; quantities, finish, condition, note, and timestamps; exact-entry deletion; idempotent wishlist membership; unsupported finishes/unknown cards; invalid quantities/notes; decoded invalid entries; corrupt/unsupported/unreadable storage; failed write rollback and retry; valid 99-copy and 500-character bounds; 500/501 emoji, ZWJ family emoji, and combining-character notes; distinct recent/name/number orderings without writes.

The two UI tests execute real navigation and entry controls: empty start, unmatched search, numeric search, mixed-case/trimmed search, every catalog card, quantity increment, reverse-holo finish, lightly-played condition, note entry, successful confirmation, relaunch restoration, add cancellation, delete cancellation, confirmed last-copy deletion, independent wishlist persistence/removal, and an actual add/save/relaunch at accessibility XXXL in dark appearance. Screenshot attachments preserve normal collection, wishlist, catalog, details, form, empty state, and dark/accessibility screens.

## Runtime-warning investigation

The full twelve-test result contained two `Invalid frame dimension (negative or non-finite)` warnings: one on note-field focus and one when the note area appeared while scrolling the accessibility-sized form. The test-result warning contexts had empty source locations and stack frames with address `0`, so they did not identify a framework or feature source line. The feature's frame values are positive constants or valid `maxWidth: .infinity` constraints; it has no computed geometry. Removing the extra keyboard toolbar's `Spacer`/Done content while retaining the note field's native Done submit key eliminated both warnings. The focused UI rerun exercised the same note-entry and accessibility scroll/save paths; its result reports two passing tests and an empty `runtimeWarnings` array. This isolates the warning-producing composition without changing the shared text-field component.

## Visual evidence and limits

Final screenshots live in `.build/verification-20260922-194923-0acb07/screenshots/` (ignored generated evidence), with semantic names mapped in `manifest.json`:

| Screen | Exported PNG |
| --- | --- |
| Empty collection | `168D48BB-9EF9-46BB-B7B7-7A1AFC535C97.png` |
| Catalog search | `4BF43FCF-AECC-4D19-BDC7-14918D9017A1.png` |
| Add copy | `E3DD176C-F4B8-4423-A26C-E523D3A045CD.png` |
| Card details | `D8E6BA81-1028-4034-92DC-A0192387CFF1.png` |
| Collection | `C439FE76-72AF-40CD-9AB8-5700EB170FEF.png` |
| Wishlist | `98574CA4-69EF-4EB5-9BC2-081C40FA4F6B.png` |
| Empty, dark/accessibility XXXL | `67865465-2365-4E60-924E-6D9B7A2DF903.png` |
| Details, dark/accessibility XXXL | `253FA0EB-7C0A-4933-B102-43138B0A8459.png` |
| Form, dark/accessibility XXXL | `87056B54-99DB-481B-8EB0-03A43FD5BDA0.png` |
| Collection, dark/accessibility XXXL | `5085A80F-B0E8-479C-B62B-CA62F38A4576.png` |
 Normal screenshots were inspected for fitted artwork, 24-point content insets, sage accents, wrapping content, and native controls. Dark/accessibility screenshots were inspected for scrolling layout and readable content; the test successfully reached and activated the form's save action.

This verifies simulator compilation and interaction on iOS 27.0. It does not establish physical-device signing, App Store distribution, iOS 17 runtime behavior, iPad interaction, VoiceOver traversal, or every device/orientation. Core component implementation is unchanged. Template doctor/build verification is recorded separately by the example integration task.

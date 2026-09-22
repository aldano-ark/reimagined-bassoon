# Pokémon collection example

## Intent and boundary

Implement the existing iOS and Android Figma collection flows as a runnable, offline example. Keep the starter projects under `apps/`, their build configuration, shared tooling, and website unchanged. Work on `codex/pokemon-collection-example`; all example runtime code, resources, build files, commands, and tests live under `examples/pokemon-collection/`.

Figma: https://www.figma.com/design/cCvOm5AHDpUjPgUlYnzuoO/?node-id=0-1

## Required behavior

- Start with an empty personal collection and wishlist. Never present the Figma placeholder totals as saved user data.
- Collection and Wishlist are native navigation destinations. Show real unique-card and total-copy counts, artwork, printed number, set, and status.
- Search collection, wishlist, and the bundled catalog by case-insensitive name, set, or printed card number. Trim surrounding whitespace; `25` finds `025`. Empty queries show all cards in the current scope. Provide explicit no-results and empty states.
- Sort saved cards by most recently added/saved, name, or printed number. Sorting does not change saved data.
- Catalog contains the seven Figma cards from set 151, clearly described as a seven-card example catalog. Bundle artwork; no network, credentials, backend, prices, scanner, or cloud sync.
- Every card opens details. Show rarity, owned quantity, and saved copy entries. Wishlist is a separate, idempotent toggle; owning a card does not silently remove it from the wishlist.
- Add a copy records quantity (1–99), an available finish, condition, and an optional note (at most 500 characters). Finishes come from the catalog. Conditions: Near mint, Lightly played, Moderately played, Heavily played, Damaged. Default: first available finish, Near mint, quantity 1.
- Save updates totals and returns visible confirmation only after persistence succeeds. Cancel leaves data unchanged. Confirm before deleting a saved copy entry; remove precisely that entry's quantity. Last-copy removal returns the collection to its empty state if appropriate.
- Persist copies and wishlist across process restarts, separately from template app storage. Write atomically. Failed writes leave the last committed state intact and expose a retryable error; corrupt/unreadable data is not silently reset or overwritten. A read failure presents Retry; deleting/resetting data is outside this example's UI scope.
- Use stable catalog IDs (`sv03.5-025`, etc.), per-entry IDs, and timestamps for saved data. Validate decoded data and reject unsupported schema versions instead of erasing user state.

## Native implementation

- iOS 17+ with SwiftUI, native NavigationStack/TabView/forms, a collection feature package, and the existing `DesignSystem` package as a read-only dependency. App entry owns dependency assembly. Separate Xcode project, checked-in shared scheme, bundle ID `com.example.pokemoncollection`.
- Android min SDK 26 / compile and target SDK 36, Kotlin/Compose Material 3, an example collection feature module, and existing `:core:designsystem` source as a read-only dependency with an example-specific build directory. Separate Gradle root and application ID `com.example.pokemoncollection`. Reuse pinned dependency versions and wrapper.
- Shared static resources at `examples/pokemon-collection/assets/` are copied into each native app by its own build system. Neither platform build invokes or reads the other platform.
- Match the Figma sage accent (`#236B51`), spacing, entire-card image aspect ratio, two-column gallery at ordinary phone text sizes, and Android's 8 dp top content padding. Respect safe areas, large text, accessibility labels, native touch targets, and dark appearance. Native chrome can adapt to the installed OS.
- Reuse supplied design-library buttons, fields, status composition, and spacing; use native controls for uncovered patterns. Add feature previews and meaningful state/UI tests without changing core components.

### Compact CTA refinement

Collection, wishlist, details, add-copy, and recovery actions use slimmer visible buttons with tighter vertical spacing. Android keeps `DSButton` and its native 48 dp interaction reservation while allowing a roughly 36 dp visual capsule at standard text size. iOS uses an example-local native button composition with a 30-point label minimum plus regular system chrome, producing a 44-point standard CTA; the shared template button's label minimum remains unchanged. Both layouts grow with larger text instead of imposing a maximum height. Keep label typography, enabled states, confirmations, and action behavior intact.

## Acceptance evidence

Build and lint the example Android app, execute its state and Compose interaction tests; build and execute the iOS example unit/UI tests on a dedicated simulator. Cover empty start, search, add/cancel, wishlist toggle, finish/condition/note persistence, restart, deletion, validation, corrupt storage, and failed writes. Visually inspect native screens when tooling permits. Run template doctor/verification commands, and show an empty diff for `apps/`, `tooling/`, and `.github/` to establish isolation. Document exact commands, outcomes, and any unverified runtime/device checks. Deliver a reviewable branch/PR; do not merge it.

# Pokémon collection example implementation plan

**Goal:** Ship runnable native collection/wishlist examples while preserving the starter apps.

**Architecture:** Independent example Gradle/Xcode projects consume collection feature modules/packages and the unchanged native design libraries. One bundled seven-card asset catalog is shared as static content; user state stays platform-local.

**Spec:** `docs/specs/2026-09-22-pokemon-collection-example.md`

## Constraints and ownership

- All runtime changes remain under `examples/pokemon-collection/`; template paths are read-only.
- Root owns catalog/assets, example README/commands, parity checks, and evidence.
- Android implementer owns only `examples/pokemon-collection/android/`.
- iOS implementer owns only `examples/pokemon-collection/ios/`.
- No concurrent builds of the same platform; template baseline verification finishes before example verification.
- iOS 17+, Android 26+/SDK 36; use repository-pinned tools/dependencies.

## Review focus

1. Relaunch after mutation must restore quantities, finish, condition, notes, and wishlist.
2. Failed/corrupt persistence must preserve committed data and never show false success.
3. Empty and unmatched searches must be distinguishable; numeric queries match padded numbers.
4. Unsupported card finishes and invalid quantities/notes cannot enter persistent state.
5. Large text and long notes must not hide primary actions; deleting one copy entry cannot remove another.

## Tasks

- [x] Establish branch, template baseline, shared catalog/artwork, and isolation contract. Both native template verifications, 47 tooling tests, and 4 asset tests passed.
- [x] Android: separate native project and collection feature. State/persistence and Compose tests observed red then green. 18 tests passed, Debug/Release lint and builds passed. Native API 36 interaction and large-text/dark visual checks completed. Review's rotation race fixed with retained ViewModel/controller and blocked-write/recreation regression.
- [x] iOS: checked-in Xcode project and collection feature package. State/persistence tests observed red then green. 10 unit and 2 UI tests passed, Debug/Release builds passed, simulator cleanup verified. Unicode note limits and large-text/dark interactions covered; final runtime-warning investigation recorded in evidence.
- [x] Integrate example-only commands, asset/parity checks, and launch documentation. Template paths unchanged; native screenshots inspected and selected images retained. Separate code review completed, rotation/write finding fixed with regression evidence. iOS keyboard-toolbar warnings resolved in a focused two-test UI run with zero runtime warnings; final Release rebuilt. Verification and launch documentation prepared for the draft PR.

## Shared catalog contract

`assets/catalog.json` is a JSON array. Each element has `id`, `name`, `number` (zero-padded string), `set` (`151`), `printedTotal` (165), `rarity`, `image` (PNG basename), and `finishes` (array of `Normal`, `Reverse holo`, or `Holo`). Android packages the directory as assets; iOS copies the directory as a bundle resource. Resource paths never point into the other platform. Stable ID and finish values are identical on both platforms.

## Behavior-test examples

Use each platform's idiomatic test API for these assertions, with a temporary/in-memory persistence implementation:

```text
initial ownedCount == 0; initial wishlistCount == 0
search("  pikACHu ") == [sv03.5-025]; search("25") == [sv03.5-025]
add(025, quantity=2, finish="Reverse holo", condition="Near mint", note="Gift")
reopen storage; totalCopies == 2; uniqueCards == 1; note == "Gift"
toggleWishlist(025); reopen; wishlist == [025]; copies remain 2
delete(entryID); totalCopies == 0; wishlist still contains 025
add(quantity=0 or 100), unsupported finish, note length=501 => reject, bytes unchanged
failed save => error visible, committed state/bytes unchanged
corrupt/unsupported schema => recoverable load error, bytes unchanged
```

Execution uses the parallel-agent skill for disjoint native implementations. Root coordinates shared assets and final review. The user's instruction to proceed authorizes implementation; no repeated design approval is introduced.

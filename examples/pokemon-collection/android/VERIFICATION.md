# Android example verification

Date: 2026-09-22.

## Executed commands

Commands below run from `examples/pokemon-collection/android/`. Android SDK and JDK locations came from the environment, not checked-in machine paths.

| Command | Result |
| --- | --- |
| `./gradlew :features:collection:testDebugUnitTest --console=plain` against initial behavior/UI stubs | Expected red: 12 tests failed, demonstrating absent collection behavior. |
| `./gradlew :features:collection:testDebugUnitTest :app:assembleDebug --console=plain` | Passed the first 12 implemented behavior/UI tests and built the launchable Debug APK. |
| `./gradlew :features:collection:testDebugUnitTest --tests '*saveFinishingDuringRecreationClosesTheDraft' --console=plain` | Expected red before the lifecycle correction: a committed save left a duplicate draft open after recreation. |
| `./gradlew :features:collection:testDebugUnitTest :app:testDebugUnitTest --console=plain` with new damaged-input regressions | Expected red: trailing JSON corruption and invalid UTF-8 were initially accepted. Both are now rejected without changing bytes. |
| `./gradlew :features:collection:testDebugUnitTest --console=plain` with new Unicode/draft-edit regressions | Expected red: UTF-16 counting rejected valid emoji notes, and a stale Retry survived editing a failed draft. Both were corrected. |
| `./scripts/verify --console=plain` after all corrections | **Passed**: 17 feature tests, 1 app Activity recreation test, feature/app Debug and Release lint, Debug and Release APK builds. |

The final entry point runs:

```sh
./gradlew \
  :features:collection:testDebugUnitTest :app:testDebugUnitTest \
  :features:collection:lintDebug :features:collection:lintRelease \
  :app:lintDebug :app:lintRelease \
  :app:assembleDebug :app:assembleRelease --console=plain
```

Final test XML reports showed 11 store tests, 6 Compose interaction tests, and 1 Activity recreation test; zero failures, errors, or skipped tests. During development the Activity test initially timed out because Robolectric's paused main looper did not advance the retained ViewModel continuation after recreation. The test now explicitly advances that looper while awaiting completion.

## Coverage

- Empty first launch; trimmed/case-insensitive/name/set/padded-number search; saved scopes; name/number sorting.
- Add/cancel; available finishes; condition/note persistence; idempotent wishlist membership; delete confirmation and precise entry removal; separate owned/wishlist state; empty and no-result displays.
- Persistence across reopening; actual atomic-file writes; committed-state preservation and retry after injected read/write failures; unsupported versions, invalid quantities/finishes/conditions, duplicate entry IDs, unknown cards, invalid timestamps, trailing JSON corruption, and invalid UTF-8.
- Quantity bounds and ASCII note bounds. For emoji, family ZWJ sequences, and combining accents, 500 user-visible characters succeed and 501 fail; the visible note counter uses the same ICU helper as validation.
- Failed saves keep the form and show no success. Editing a failed draft clears its stale retry candidate; the next save commits the edited values.
- Saved instance state retains an unsaved draft. A save completing during UI recreation closes that draft. Actual Activity recreation during a blocked mutation retains its ViewModel/controller; a subsequent wishlist mutation preserves the first copy commit when data is reopened from disk.

The Figma context and screenshots were loaded for Android nodes `13:99`, `13:113`, `13:127`, `13:141`, `13:155`, and `13:169` in file `cCvOm5AHDpUjPgUlYnzuoO`. Adaptations use the existing native design components, actual persisted totals, complete bundled artwork, sage/neutral light and dark palettes, and native navigation/form controls. Tabbed content starts at 8 dp. Gallery content, including search/headers, scrolls on compact displays; larger text switches saved-card galleries to one column.

## Artifacts and limits

- Test reports: `features/collection/build/reports/tests/testDebugUnitTest/` and `app/build/reports/tests/testDebugUnitTest/`.
- Lint reports: `features/collection/build/reports/lint-results-{debug,release}.html` and the corresponding `app/` reports.
- APKs: `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release-unsigned.apk`.
- Lint completed with **zero errors**. Warnings identify intentionally pinned SDK/dependency versions, missing custom launcher artwork, and missing Android 12 backup-rule metadata. No dependency/SDK upgrade or warning suppression was introduced.
- JDK 25 emitted Robolectric native-access warnings; test execution passed. Robolectric uses API 35. Minimum-OS hardware, physical devices, and TalkBack traversal were not exercised by this implementation worker.
- A coordinating worker performs native visual/interaction checks on a separately created API 36 emulator. Its screenshots and final runtime findings are recorded in the example-level verification evidence. Those checks are distinct from the automated tests above.
- Android Studio interactive import/preview rendering and signed Release installation were not verified. Debug preview sources compile with the feature.
- No files under `apps/`, `tooling/`, or `.github/` were changed by this implementation. The design-library source dependency writes generated output only into this example's ignored `build/designsystem/` directory.

# Pokémon example demo

The landing page and PR #3 use the same silent recording of the actual iOS example. This supports the monorepo introduction; it does not replace the starter apps or present the Pokémon sample as the main project.

## Capture

The capture uses `xcrun simctl io <owned-simulator> recordVideo --codec=h264 <movie>` while the existing XCTest collection flow drives the native app. No computer-use tool, fabricated interface, seeded ownership, or private user device is involved. XCTest uses its isolated storage file, and the recording run creates and deletes its own simulator.

The source interaction is `CollectionUITests/testAddSearchWishlistRelaunchAndConfirmedDeletion`. The public demo selects the successful search, wishlist, add-copy, confirmation, and retained-collection portion of that flow. Test-only searching for an unknown card, removal checks, and startup/relaunch waiting are omitted. Playback remains at normal speed. The caption and written walkthrough identify reopening the app rather than implying that the transition was a navigation tap.

## Published assets

- `site/assets/pokemon-collection-demo.mp4`: 35 seconds, 2,050,842 bytes, 720×1566, H.264/yuv420p at constant 30 fps. The MP4 index precedes video data for playback before the whole file is downloaded.
- `site/assets/pokemon-demo-poster.webp`: an actual saved-collection frame.
- `site/assets/pokemon-demo-captions.vtt`: optional English step captions.

Native capture can have variable timestamps and omit idle frames. Normalize timestamps/frame rate before choosing edits, and fully decode the final MP4 as a playback check. Inspect representative final frames, verify caption ranges against the encoded duration, and preserve the original capture and edit manifest in ignored local evidence.

The page uses native controls, `playsinline`, `preload="none"`, and no autoplay. A text walkthrough and MP4 download are available beside the player. The Android preview is a screenshot of the actual independent example app. Example links point to the feature branch/PR while it awaits merging.

## Attach to the PR

GitHub CLI supports uploading a video with `gh pr edit --attach`. Put a reference to the local MP4 alone in a paragraph of the body file:

```markdown
![](site/assets/pokemon-collection-demo.mp4)
```

Then update the body and attach the same file:

```sh
gh pr edit 3 --body-file <prepared-body.md> --attach site/assets/pokemon-collection-demo.mp4
```

The CLI replaces the reference with a GitHub-hosted attachment that renders as an inline player. Fetch and preserve the existing PR body before editing; verify the returned attachment URL after upload. See [GitHub's attachment instructions](https://docs.github.com/en/github-cli/github-cli/attaching-files-with-github-cli).

Publish `site/` through the existing [GitHub Pages workflow](landing-page.md). Confirm both the deployment result and public video playback. Native app code and template build configuration are unchanged by the demo publication.

## Verification — 2026-09-22

- Recorded the unmodified example at commit `c4afd2e` on a newly created iPhone 18 Pro / iOS 27.0 simulator. The selected XCTest passed 1/1 with no runtime warnings. Recorder exit and owned-simulator cleanup both succeeded; the device was absent from the final inventory.
- Raw recording: 70.118 seconds at 1206×2622. Normalized the variable timestamps, retained the genuine successful-flow segments, and added brief holds on real recorded frames. Moving footage remains at 1× speed. Captions explain the app reopening.
- Final MP4: 1,050 frames, 35 seconds, no audio. Full `ffmpeg` decode passed without errors; `ffprobe` confirmed encoding and dimensions. MP4 atom order and all eight ordered, non-overlapping VTT cues passed checks. Eighteen representative frames and the saved-collection poster were visually inspected.
- Landing-page static checks passed for unique IDs, anchor targets, local assets, video configuration, JavaScript syntax, and whitespace. Chromium played the file after an actual Play click, decoded 720×1566 video, and loaded all eight captions. Pause and the keyboard-operated transcript worked. Initial load made no MP4 request, and autoplay is absent.
- Layouts at 320, 768, and 1440 px had no horizontal overflow. The 320 px video viewport was 264 px wide with full controls; desktop retains the paired native previews. Browser console had no warnings or errors. Hosted seeking and final deployment are verified after publishing.

Original capture, logs, frame inspection, edit manifest, and native result bundle are retained locally in `/tmp/pokemon-demo/` as task evidence, outside tracked source. Published media, captions, and this provenance are tracked; machine-specific paths and simulator identities are not required to consume them.

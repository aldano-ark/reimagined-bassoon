# Landing page verification — 2026-09-22

Scope: `site/`, repository introduction copy, and hosting documentation. No native app, component, or shared build tooling changed.

## Results

- `node --check site/assets/site.js`: passed.
- `git diff --check`: passed after removing an extra trailing blank line in the stylesheet.
- HTMLParser checks of local asset paths, unique IDs, in-page links, and ARIA references: passed.
- HTTP checks of all ten unique GitHub repository/documentation links: 200 responses.
- Live Chromium inspection at 1440, 768, 393, and 320 CSS pixels: no horizontal page overflow. A 320-pixel platform-label overflow found during inspection was corrected by allowing label wrapping and flex-item shrinking.
- Desktop and phone screenshots visually inspected. All three images and the self-hosted Geist font load. Offscreen phone images load when their section is approached; full-page screenshots taken before scrolling can omit these lazy images.
- iOS/Android setup tabs: pointer selection and ArrowLeft keyboard navigation update the selected tab, focus, and visible panel correctly.
- Copy handler: both command payloads verified against their panels; real clipboard write returned “iOS commands copied.” A deliberately rejected clipboard write selected the correct commands and announced the fallback.
- Browser console: no errors or warnings. The image, stylesheet, script, and font requests returned 200.
- GitHub Pages build and deployment: successful, including [the responsive fix deployment](https://github.com/aldano-ark/reimagined-bassoon/actions/runs/35714535723).
- Figma desktop and mobile layouts: screenshots reviewed. Mobile sections grow with their content. Overflow inside the two example phone screens belongs to their intentionally clipped scroll areas, not the landing layout.

## Limits

The local browser handoff was unavailable, so browser inspection used the live GitHub Pages URL after publication. These checks used Chromium; Safari, Firefox, a screen reader, and physical devices were not tested. Reduced-motion handling is present in CSS; an emulated reduced-motion browser run was not performed. Native doctor/build/test commands were not run because this change does not affect either native app.

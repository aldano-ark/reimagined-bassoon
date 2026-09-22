# Monorepo introduction page

Introduce the Native Mobile Template to developers evaluating or adopting it. The page presents the monorepo itself; the Pokémon collection is a supporting native example, developed separately from the reusable starter apps.

The visual direction is calm and spacious: a lakeside dawn image, blue and green platform accents, Geist typography, and paired native previews. The iOS preview plays a real app walkthrough on demand; the Android preview uses an actual app screenshot. “native / together” is the landing-page wordmark; repository and native project names remain unchanged.

The page explains independent native builds, shared product requirements, native design libraries, environment configurations, and verification. It links directly to the repository and existing documentation. Its quick-start section switches between iOS and Android commands and lets readers copy the selected command block, with a selection fallback if clipboard access fails.

Deliverables are editable desktop and mobile designs on the existing Figma file’s `03 · Monorepo landing` page, responsive static assets under `site/`, and GitHub Pages hosting. No app runtime, native project, build tool, or product behavior changes are included.

Acceptance criteria:

- The first viewport identifies a monorepo for native iOS and Android apps.
- Pokémon appears only in the supporting example section. Its preview label and links identify the example branch/PR while it awaits merging; the Figma study remains linked.
- Layouts work at phone, tablet, and desktop widths without horizontal page overflow.
- Links, keyboard tab selection, copy behavior, and all local assets work.
- The site works at a GitHub Pages repository subpath and requires no build dependencies or external runtime assets.
- The page respects reduced-motion preferences and uses semantic headings, accessible controls, and image descriptions.
- The real, silent iOS demo has native video controls, inline playback, no autoplay, and no eager video download. A poster, optional timed captions, written walkthrough, and direct MP4 download make the demo usable across viewing preferences. Phone layouts give the video enough width for controls.

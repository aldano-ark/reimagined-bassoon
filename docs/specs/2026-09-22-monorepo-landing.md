# Monorepo introduction page

Introduce the Native Mobile Template to developers evaluating or adopting it. The page presents the monorepo itself; the Pokémon collection is a supporting Figma design study, not a shipped application feature.

The visual direction is calm and spacious: a lakeside dawn image, blue and green platform accents, Geist typography, and paired native screenshots. “native / together” is the landing-page wordmark; repository and native project names remain unchanged.

The page explains independent native builds, shared product requirements, native design libraries, environment configurations, and verification. It links directly to the repository and existing documentation. Its quick-start section switches between iOS and Android commands and lets readers copy the selected command block, with a selection fallback if clipboard access fails.

Deliverables are editable desktop and mobile designs on the existing Figma file’s `03 · Monorepo landing` page, responsive static assets under `site/`, and GitHub Pages hosting. No app runtime, native project, build tool, or product behavior changes are included.

Acceptance criteria:

- The first viewport identifies a monorepo for native iOS and Android apps.
- Pokémon appears only in the supporting example section, labeled as a Figma concept.
- Layouts work at phone, tablet, and desktop widths without horizontal page overflow.
- Links, keyboard tab selection, copy behavior, and all local assets work.
- The site works at a GitHub Pages repository subpath and requires no build dependencies or external runtime assets.
- The page respects reduced-motion preferences and uses semantic headings, accessible controls, and image descriptions.

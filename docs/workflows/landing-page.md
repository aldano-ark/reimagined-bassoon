# Project landing page

The [landing page](https://aldano-ark.github.io/reimagined-bassoon/) introduces this monorepo. Source files live in `site/`; the native applications and their tooling are independent of it.

Designs: [desktop](https://www.figma.com/design/cCvOm5AHDpUjPgUlYnzuoO/?node-id=40-409) and [mobile](https://www.figma.com/design/cCvOm5AHDpUjPgUlYnzuoO/?node-id=44-486), on `03 · Monorepo landing`. The web implementation follows that visual direction with responsive layouts, repository links, keyboard-operable setup tabs, and command copying.

## Preview

No build or package install is required. From the repository root:

```sh
python3 -m http.server 4173 --directory site
```

Open `http://localhost:4173`. Assets use relative URLs, so the same files work under `/reimagined-bassoon/` on GitHub Pages. The site makes no external asset requests and stores no visitor data.

## Publish

GitHub Pages publishes the root of `gh-pages`. Only the contents of `site/` belong on that branch. The `.nojekyll` file keeps the page as plain static files.

After reviewing and committing changes to `site/`, publish its history with:

```sh
git subtree push --prefix site origin gh-pages
```

This updates the publishing branch without changing the current checkout or publishing the rest of the monorepo. Use a normal fast-forward update; investigate rejected updates rather than force-pushing. In repository Settings → Pages, the source should be **Deploy from a branch**, **gh-pages**, **/(root)**. GitHub’s Pages build/deployment run records publication status.

For a fork, update repository URLs in `site/index.html`, the quick-start clone commands, and the links in these docs before publishing to the fork’s Pages URL. The visual wordmark does not rename the native projects.

## Assets

- `assets/lakeside-dawn.webp`: original landscape generated for this page with OpenAI image generation; compressed from the generated PNG.
- `assets/ios-collection.webp` and `assets/android-collection.webp`: exports of the existing Figma collection concepts, nodes `13:12` and `13:99`. Pokémon card imagery in those concepts came from [TCGdex](https://www.tcgdex.net/). These are supporting design examples, not evidence of implemented app features.
- `assets/geist-variable.ttf`: Geist from [Google Fonts](https://github.com/google/fonts/tree/main/ofl/geist), self-hosted under the included `assets/Geist-OFL.txt` license.
- `assets/favicon.svg`: the page’s paired-frame mark. Interface icons are simple inline SVG paths.

## Verification

Check JavaScript syntax with `node --check site/assets/site.js`, verify local assets and anchor targets, and inspect the page at narrow mobile, tablet, and desktop widths. Exercise both setup tabs with pointer and keyboard, copy each command block, and check the clipboard-failure fallback. Confirm reduced-motion behavior and browser console/network results. Publishing succeeds only after the hosted Pages deployment completes and the public URL serves the intended content.

Native checks are not applicable to a change confined to `site/` and explanatory Markdown; no native or shared build files are involved.

See the [measured landing-page verification](landing-page-verification-2026-09-22.md) for results and limitations.

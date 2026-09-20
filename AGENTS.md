# Contributor instructions

## Find the relevant context

- Architecture and boundaries: [docs/architecture/overview.md](docs/architecture/overview.md).
- Toolchain prerequisites: [docs/architecture/toolchains.md](docs/architecture/toolchains.md).
- Native UI foundations and components: [docs/design-system/README.md](docs/design-system/README.md).
- Development and evidence: [docs/workflows/development.md](docs/workflows/development.md), [docs/workflows/verification.md](docs/workflows/verification.md).
- Read `apps/android/AGENTS.md` or `apps/ios/AGENTS.md` when changing that platform.
- Product behavior belongs in `docs/specs/`; use `tooling/templates/` for new work definitions and decisions.

## Work within the task

Use the user's stated objective, scope, and completion boundary. Inspect existing conventions before editing. Continue routine implementation and verification within the authorized scope; request missing information only when it changes correctness or blocks the work. Do not invent product behavior to populate empty modules.

Use separate checkouts/worktrees or explicitly disjoint file ownership for concurrent contributors. Coordinate changes to shared contracts, build manifests, and guidance.

## Preserve native boundaries

Each platform must remain independently buildable. App entry points assemble features and dependencies. Features can depend on core; core cannot depend on features or the app. A feature cannot import another feature's implementation. Shared behavior does not require identical native types or file layouts.

Add modules, packages, data layers, and third-party dependencies for concrete responsibilities. Keep machine paths, credentials, generated outputs, and IDE user state out of tracked files.

## Verify and report

Run `./tooling/scripts/doctor <platform>` to inspect prerequisites and `./tooling/scripts/verify <platform>` for the affected native app. Changes to shared tooling require `python3 -m unittest discover -s tooling/tests -v`; shared build changes also require both native verifications when toolchains are available.

Use the design library for its supplied patterns, and native controls for uncovered behavior. Component changes need the corresponding behavior tests and preview examples. Android verification executes component tests; iOS verification compiles its UI tests, which must be run separately on a dedicated simulator for interaction evidence.

Document a change's behavior, affected files/platforms, commands and results, and any remaining limitations. Missing tools or unrun checks are unverified, never a pass. Build, lint, IDE import, and simulator launch are distinct forms of evidence. Keep architecture decisions in `docs/decisions/` when a boundary changes.

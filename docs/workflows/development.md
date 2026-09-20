# Development workflow

## Start a work item

Define the objective, affected platforms, acceptance criteria, scope, and authorized completion boundary using [the work-item template](../../tooling/templates/work-item.md). New product behavior can use [a feature spec](../../tooling/templates/feature-spec.md). Read the root and relevant platform contributor instructions.

An assisted task may need design input as work progresses. An autonomous task needs enough requirements and access to finish its agreed scope. Both use the same project files and checks. A task's existing authorization covers routine work within that scope; the template adds no repeated approval steps.

## Implement

Inspect the affected code and build dependencies. Keep changes scoped to the work item. Add meaningful tests for behavior and fixes; native builds/lint are the initial empty-shell checks. Introduce libraries, use cases, and frameworks when a concrete responsibility warrants them.

Use a branch and, when isolation is useful, a separate checkout/worktree. Concurrent contributors own disjoint files or separate checkouts and coordinate edits to shared contracts and manifests.

## Verify and hand off

Run the affected platform's verification command. Shared tooling changes require command tests and both native checks when the environment provides the toolchains. Separate compilation, static analysis, IDE checks, and launch evidence.

Record commands, results, artifacts, and unverified checks in the work item. Describe what changed and why. Leave a clear next action if an external dependency blocks completion. Record consequential architecture changes with [a decision](../../tooling/templates/decision.md).

## Release and automation

The initial template defines local development checks. Configure your chosen CI provider to invoke these same commands on suitable runners: Android can run independently of macOS; iOS requires macOS and Xcode. Add signing, environments, and release workflows when needed. An agent runner can schedule work items and consume their evidence without changing the native project structure.

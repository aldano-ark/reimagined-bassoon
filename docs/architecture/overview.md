# Architecture

One product lives in independent native platform projects. Shared documents describe what the product does; platform projects decide how to deliver that behavior with native APIs.

## Responsibilities

| Boundary | Owns | May depend on |
| --- | --- | --- |
| App | Startup, dependency assembly, root navigation | Features and core |
| Feature | Cohesive behavior, presentation, state, feature-specific logic | Core and its own internal code |
| Core | A narrowly defined shared capability or interface | Other acyclic core dependencies |

Core cannot import features or the app. A feature cannot import another feature's implementation. Coordinate feature navigation at the app boundary; extract a small shared interface only when a consumer needs it.

Android expresses build boundaries with Gradle modules and Kotlin visibility. iOS uses local Swift packages, target dependencies, and Swift access control. Initially each platform has one app target/module. Reserved directories describe extension points without adding dummy compiled libraries.

## State and data

Screens render explicit state and send actions to feature-owned state holders. Android can use ViewModels; iOS can use Swift observation with appropriate actor isolation. Follow platform idioms rather than requiring the same classes across languages.

When external data exists, isolate access behind repositories or service interfaces. Add a domain/use-case layer for business rules that benefit from that separation. Model recoverable failures as explicit state with a recovery action. The empty shell has no external services or data layer.

## What is shared

- Product requirements and acceptance criteria in `docs/specs/`.
- Language-neutral schemas and fixtures in `contracts/` when needed.
- Architecture decisions, contribution workflow, and verification commands.

Runtime implementations, native dependency manifests, and IDE projects belong to their platforms. Product parity means equivalent required behavior, not identical code or screen structure.

## Build isolation

Android builds under `apps/android/`; iOS derived data is under `.build/ios/`. Logs are separated by platform. Neither native project references the other. Shared shell commands validate their arguments before dispatch and inspect only requested toolchains.

One build per platform can run concurrently. For simultaneous work on the same native target, use distinct checkouts/output directories. A build must not rewrite tracked sources or select SDKs globally.

See [the initial decision](../decisions/0001-independent-native-projects.md) and [toolchain baseline](toolchains.md).

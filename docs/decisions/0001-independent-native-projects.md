# 0001: Independent native projects with feature-oriented growth

Status: Accepted, 2026-09-20.

## Context

The template serves one future product on Kotlin/Android and Swift/iOS. No product behavior is defined yet. Humans and agents need clear boundaries and repeatable checks.

## Decision

Keep independent native projects under `apps/`. Use app, feature, and core responsibilities inside each platform. Start with one app module/target, adding libraries when real responsibilities emerge. Share requirements, schemas, conventions, and command entry points at the repository root.

## Alternatives

Bare platform folders would leave ownership and verification conventions to each new project. A fully prebuilt layered architecture would introduce speculative packages and frameworks. A shared runtime would add a technology choice beyond the requested native language implementations.

## Consequences

Each platform can build and evolve with its native tools. Product behavior must be kept consistent through shared requirements and evidence. Some implementation logic will be duplicated across languages. Dependency boundaries become enforceable as real Gradle modules and Swift packages are introduced.

## Validation

Build both empty applications independently, sequentially, and concurrently. Confirm distinct artifacts, platform-specific prerequisites, correct failure statuses, and unchanged tracked build inputs. See [verification guidance](../workflows/verification.md).

# 0002: Native environment configuration

Status: Accepted, 2026-09-21.

## Context

Developers and testers need dev, staging, and production apps installed together. This template must demonstrate how an environment's endpoint reaches application code while preserving independent Android and iOS builds.

## Decision

Use Android product flavours backed by platform-local properties files and iOS shared schemes/build configurations backed by platform-local xcconfig files. Keep environment selection separate from Debug/Release. Use distinct identifiers for dev/staging and the unsuffixed identifier for production.

App startup validates compiled configuration and injects the base URL into an app-owned request factory. The template displays the resulting health request without sending it. Configuration and the example do not require a new core module, Swift package, network dependency, or root generator.

Keep public values checked in. Share their behavioral contract through the [spec](../specs/2026-09-20-native-environments.md), and check native configuration parity in the shared test suite. Native compilation does not depend on that suite or the other platform.

## Alternatives

Shared .env files would need integration with both build systems; the user chose native configuration. Runtime switching would add state and isolation concerns beyond the selected build-time identities. A shared configuration generator would add a dependency to otherwise independent builds.

## Consequences

Each IDE can build its platform directly. Equivalent public values are maintained twice, and endpoint changes require rebuilding. Debug and Release of one environment share its identity and remain subject to native signing rules.

The root build commands default to dev Debug. Verification covers all six variants per platform and checks artifact metadata. iOS verification compiles tests; simulator test execution remains separate.

AGP resource generation and Release host tests are enabled explicitly. Kotlin AppConfig is a plain immutable class with a private constructor, avoiding an unchecked generated copy method.

## Validation

See the [measured verification record](../workflows/native-environments-verification-2026-09-21.md) for build/test results, simultaneous installation, endpoint-edit examples, build isolation, and remaining limits.

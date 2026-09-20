# HarmonyOS integration point

HarmonyOS support is planned; there is no native project or build command here yet. `all` currently means Android and iOS.

When adding support, record a platform decision covering the supported HarmonyOS generation, native language/UI tooling, module structure, SDK/toolchain versions, host requirements, and test strategy. Implement the same product behavior contracts while preserving native platform conventions.

Add a platform README and `AGENTS.md`, independent native build inputs, platform-specific output paths, and doctor/build/verify adapters with tests. Extend `all` only after that platform has a working build and explicit prerequisites. Adding HarmonyOS must not make Android or iOS depend on its SDK.

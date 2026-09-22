# 0003 — Isolated native example apps

Status: accepted for the requested example implementation.

The user wants the Pokémon collection design implemented without changing the reusable starter. Keeping it only on a branch would replace the starter if merged. Place independently buildable projects under `examples/pokemon-collection/android` and `examples/pokemon-collection/ios`, on a feature branch as well.

Each app owns startup and consumes its own collection feature. Both depend on the existing platform design library without editing it. Common card metadata/artwork is static example content, packaged independently by Gradle and Xcode. Build output, app identities, and persisted collection data are separate from the starter.

Example commands and tests remain under the example directory; the root template commands and CI retain their existing behavior. The tradeoff is a second pair of native project manifests to maintain. The example is runnable from a repository checkout, not a standalone exported folder, because it reuses the native design libraries.

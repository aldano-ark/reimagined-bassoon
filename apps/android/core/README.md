# Android core

The [designsystem library](designsystem/README.md) supplies native Material 3 foundations, common components, previews, and component tests. Consumers depend on `:core:designsystem`.

Add shared capabilities here only when a real consumer needs them. Each module must have one stated responsibility, a small interface, and explicit Gradle dependencies.

Core cannot depend on features or the app. Prefer Kotlin/JVM modules for code with no Android resources or APIs; use Android libraries when those platform capabilities are required. Keep unrelated helpers out of a catch-all utilities module.

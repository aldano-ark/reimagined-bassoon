# Android features

Add a Gradle library module here when a cohesive product feature exists, for example `features/<feature-name>`. Keep its UI, state, feature-specific logic, resources, and tests together.

Register it in `settings.gradle.kts` and add it to the application dependencies. Features may depend on narrowly scoped core modules, but cannot import another feature's implementation or the app. Coordinate navigation and dependency assembly in `app/`.

Use Kotlin `internal` visibility for implementation details and expose only the interfaces consumers need. Add repositories and use cases when behavior requires them. This directory intentionally has no compiled module yet.

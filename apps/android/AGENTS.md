# Android contributor instructions

Follow the root `AGENTS.md` and this platform's `README.md`.

- Keep Kotlin and Compose implementation here. The Gradle build cannot depend on Xcode, iOS sources, or root scripting for compilation.
- Pin dependencies in `gradle/libs.versions.toml`; keep the official wrapper and distribution checksum checked in. AGP supplies Kotlin support, so do not add a competing Kotlin Android plugin.
- Put startup, root navigation, and dependency assembly in `app/`. Add feature/core modules according to their directory READMEs. Prefer `internal` visibility and `implementation` dependencies.
- Use state holders appropriate to the feature; add domain and data abstractions when behavior warrants them.
- Keep SDK paths in the environment or ignored `local.properties` and signing material out of Git.
- For app/build changes, run `./tooling/scripts/verify android` from the repository root. Native equivalents and artifacts are in `README.md`. Introduce meaningful behavior tests with product features.

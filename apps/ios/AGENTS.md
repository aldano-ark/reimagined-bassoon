# iOS contributor instructions

Follow the root `AGENTS.md` and this platform's `README.md`.

- Keep Swift and SwiftUI implementation here. The Xcode build cannot depend on Gradle, Android sources, or root scripting for compilation.
- Commit project changes and the shared scheme; ignore `xcuserdata`. A new checkout must build without regenerating the project.
- Put startup, root navigation, and dependency assembly in `App/`. Introduce local feature/core packages according to their directory READMEs; expose small public interfaces.
- Use Swift observation and actor isolation deliberately. Keep implementation details internal and external services behind appropriate interfaces when introduced.
- Use the local `DesignSystem` package for supplied patterns; see `Packages/Core/DesignSystem/README.md`. Preserve inherited native styling and caller-owned state. Keep the preview gallery and test host under debug compilation.
- Keep `DEVELOPER_DIR` selection command-scoped. Simulator verification must not require a personal team or distribution credentials.
- For app/build changes, run `./tooling/scripts/verify ios` from the repository root. It compiles the app/package and UI-test target; interaction evidence requires a separate `DesignSystemUITests` run on a dedicated simulator. Native equivalents and artifacts are in `README.md`. Introduce meaningful behavior tests with product features.

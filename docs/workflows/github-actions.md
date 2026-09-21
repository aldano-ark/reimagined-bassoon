# GitHub Actions

[Native CI](../../.github/workflows/native-ci.yml) runs on pull requests, pushes to `main`, and manual dispatch. Three independent jobs invoke the same commands used locally. New runs cancel older runs for the same pull request or branch.

| Check | Runner and setup | Commands |
| --- | --- | --- |
| Tooling | Ubuntu 24.04, Python 3.12 | `python3 -m unittest discover -s tooling/tests -v` |
| Android | Ubuntu 24.04, Temurin 25, Android platform 36/build tools 36.0.0 | `doctor android`, `verify android` |
| iOS | GitHub's `xcode-27` Apple Silicon image, Xcode 27, Python 3.12 | `doctor ios`, `verify ios`, `test ios all` |

All native command paths are under `tooling/scripts/`. Android verification covers all six variants, app tests, lint, and design-system tests. iOS verification builds all six configurations and compiles the Debug tests; the separate test command executes app and UI tests for dev, staging, and production on a disposable simulator.

The iOS image is currently a [GitHub public preview](https://github.com/actions/runner-images/issues/14404). Its [software manifest](https://github.com/actions/runner-images/blob/main/images/macos/xcode-27-arm64-Readme.md) defines the available Xcode and runtimes. The workflow explicitly selects its Xcode 27 alias through `DEVELOPER_DIR`, without changing global Xcode selection. Update that selection and reverify when moving to a newer image. The runner's preview status can affect capacity and availability; local Xcode verification remains a separate form of evidence.

## Results and artifacts

Each job uploads its available evidence even after a failed check. GitHub retains artifacts for 14 days:

- `tooling-evidence`: Python test output.
- `android-evidence`: prerequisite/verification logs, lint reports, and JUnit XML/HTML test reports.
- `ios-evidence`: prerequisite/verification logs, per-environment XCTest logs, result summaries, and `.xcresult` bundles. DerivedData is excluded to keep uploads focused on verification evidence.

Download artifacts from the workflow run's summary page. Open downloaded `.xcresult` bundles in Xcode to inspect individual tests. A missing artifact or skipped test step is not a passing test. Job timeouts are 10 minutes for tooling, 40 for Android, and 90 for iOS; the iOS runner also enforces its own native-command timeouts and cleanup.

## Configuration and maintenance

The workflow needs no repository secrets, distribution certificates, or signing team. Its token has read-only repository access, checkout credentials are not retained, and action references use immutable commit pins with release comments. Gradle setup validates the checked-in wrapper and uses its basic cache provider. Builds continue to use the checked-in Gradle wrapper and platform configuration files.

To rerun a failed check, use the run's **Re-run jobs** control. Once the workflow exists on the default branch, **Run workflow** also permits a manual run. If branch rules should require CI before merging, select the stable check names `Tooling`, `Android`, and `iOS` in the repository's rules; this workflow does not change repository access or branch policies.

When changing CI, validate the YAML with `actionlint`, run the shared tooling suite and affected native verification commands, then inspect an actual GitHub-hosted run. YAML validation and local native results do not establish hosted-runner success. See [verification guidance](verification.md) for local commands and evidence distinctions.

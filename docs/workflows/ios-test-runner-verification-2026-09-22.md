# iOS test runner verification — 2026-09-22

## Objective and completion boundary

Provide a repeatable local command that creates a dedicated iOS simulator, executes the existing app and component tests, retains evidence, and cleans up. The authorized outcome is an implemented and verified local change. CI and release automation are separate work.

## Behavior and affected files

`./tooling/scripts/test ios [dev|stg|prod|all]` defaults to dev. `all` runs the three Debug environments sequentially and stops at the first failure. The POSIX entry point validates arguments and launches a Python standard-library runner. It reuses the existing iOS doctor, selects an available compatible iPhone/runtime, and uses explicit destinations, isolated DerivedData, ad-hoc signing, and disabled parallel testing.

Each run retains its lifecycle log, environment logs, result bundles, and JSON summary in a unique ignored `.build/ios-tests/<run>/` directory. Success requires both a successful native command and an XCTest result summary reporting passing, executed tests. Native failure codes survive cleanup. Shutdown and deletion target only the run-owned device; interruption first terminates the active native process group. Partial creation can recover its device by the unique run name. A deletion failure makes an otherwise successful run fail.

Implementation is in `tooling/scripts/test` and `tooling/scripts/ios_tests.py`; command behavior tests are in `tooling/tests/test_ios_runner.py`. Root/iOS contributor guides, READMEs, toolchain guidance, design-library guides, and verification guidance document the command. Native source, project manifests, and existing build/verify dispatch are unchanged.

## Prerequisites and acceptance criteria

- Python 3.9+ with its standard library, selected Xcode, and an installed available iOS Simulator runtime meeting `Config/Base.xcconfig`'s deployment minimum.
- Execute AppTests and DesignSystemUITests for a selected environment or all three.
- Preserve logs and results across repeated runs and failures.
- Never select or delete an existing personal simulator.
- Preserve native failure status, report cleanup failures, and clean up after normal completion or handled interruption.
- Keep existing Android and iOS verification working independently.

## Commands and measured results

Host: macOS 27.0, Apple Silicon. Native tests selected Xcode 27.0 (27A266a), iOS 27.0, and a newly created iPhone 18 Pro simulator. Android doctor selected JBR 25.0.3 and SDK/build tools 36/36.0.0.

| Command or check | Result |
| --- | --- |
| `./tooling/scripts/doctor ios` | Passed, exit 0 |
| `./tooling/scripts/doctor android` | Passed, exit 0 |
| `python3 -m unittest discover -s tooling/tests -v` | 47 tests passed, exit 0 |
| `./tooling/scripts/test ios all` | 45 XCTest executions passed, zero failures/skips, exit 0 |
| Dev Debug XCTest | 7 app tests and 8 component UI tests passed |
| Staging Debug XCTest | 7 app tests and 8 component UI tests passed |
| Production Debug XCTest | 7 app tests and 8 component UI tests passed |
| Simulator cleanup | Runner reported deletion; a separate `simctl list -j devices` confirmed its UUID was absent |
| `./tooling/scripts/verify ios` | All six configurations passed; Debug compiles test targets, exit 0 |
| `./tooling/scripts/verify android` | Six-variant build/lint/test dispatch and library checks passed, exit 0; Gradle reused up-to-date compilation/test outputs |
| Shell/Python syntax and `git diff --check` | Passed |

The original shared suite passed 35 tests before implementation. Eleven new runner tests initially failed because the command did not exist, then passed after implementation. An independent read-only review found no Critical or Important issues and suggested additional coverage for partial simulator creation. That case was added and the final complete suite passed all 47 tests.

The 12 runner tests invoke the real command against fake external native tools. They cover paths containing spaces and invocation from another directory, default/selected/all environments, retained results across repeated runs, invalid arguments/prerequisites, runtime compatibility, native and boot failures, partial simulator creation, deletion failures, missing/empty execution evidence, and SIGTERM while a native process is running. The simulated pre-existing personal device remains untouched.

## Retained evidence

These artifacts are ignored local outputs, not portable Git content:

- `.build/ios-tests/20260921T232626Z-o995p1t8/`: successful real run, with `run.log`, `summary.json`, and `dev`, `stg`, and `prod` logs/result bundles/DerivedData.
- `.build/ios-tests/tooling-tests.log`: final 47-test command suite output.
- `.build/ios-tests/verify-ios.log`: complete iOS verification output.
- `.build/ios-tests/devices-after.json`: separate device listing confirming cleanup.
- `.build/logs/{android,ios}/{doctor,verify}.log`: existing command logs, replaced by subsequent invocations.

The run directory timestamp is UTC; its local verification date is September 22 in Asia/Kuala_Lumpur.

## Limits and handoff

Real execution and cleanup were validated on Xcode/iOS 27.0. Error injection and termination behavior were validated at the external-tool boundary, not by interrupting the real XCTest matrix. Other Xcode/runtime combinations, screen-reader interaction, and physical-device behavior were not exercised. Xcode emitted debugger-version lookup notices during UI tests; all assertions passed.

SIGKILL and host shutdown cannot trigger cleanup. The lifecycle log records the simulator name/UUID for manual recovery. Test evidence and DerivedData intentionally remain until their run directory is removed. No signing credentials or runtime installation are required by the runner beyond an already available simulator runtime.

There are no remaining blockers for this local command. Use `./tooling/scripts/test ios` for the default dev checks and `./tooling/scripts/test ios all` for the environment matrix.

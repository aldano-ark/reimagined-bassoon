# 0003: Independent Huawei HarmonyOS foundation

Status: Proposed for written review, 2026-09-21. Implementation is pending.

## Context

The repository reserves `apps/harmonyos/` beside independent Android and iOS projects. The selected next milestone is a buildable Huawei HarmonyOS phone/tablet app with native tooling and repository command integration. Environment and design-library parity are deferred.

DevEco Studio 6.1.1.300 and its bundled HarmonyOS 6.1.1.125 SDK (API 24) are installed. Installation metadata is available; native build and runtime evidence must still be collected.

## Decision

Create a standard Stage-model ArkTS/ArkUI project with one `entry` app module under `apps/harmonyos/`. Keep its native configuration, dependencies, resources, and IDE inputs independently usable. Use the installed API 24 toolchain as the first build and compatibility baseline.

The first screen displays the application name using native UI. Add feature and core libraries only when concrete behavior warrants them. Future modules retain the existing app/feature/core dependency direction.

Extend the current shared dispatcher with thin HarmonyOS doctor/build/verify handling. Select bundled tools through installation discovery or an explicit process-scoped override. Debug and Release verification uses unsigned packages and native lint; device signing and launch remain separately documented checks. Do not introduce a shared runtime or a repository-wide Node dependency for other platforms.

Enable HarmonyOS in `all` after successful native verification establishes support. Aggregate commands then attempt all three platforms and fail when a requested platform's prerequisites are missing. Explicit Android/iOS commands remain independent of HarmonyOS.

The [foundation specification](../specs/2026-09-21-harmonyos-foundation.md) defines tool selection, artifacts, failure handling, acceptance criteria, and evidence requirements.

## Alternatives

- Split the shared dispatcher into per-platform adapters immediately. This could help additional platform growth, but broadens a small integration into a tooling refactor. The existing dispatcher remains adequate for this milestone.
- Deliver environment and design-library parity in the same change. This enlarges the implementation and testing scope before the native build foundation has been established. The user selected staged parity instead.
- Support OpenHarmony alongside Huawei HarmonyOS. That introduces a separate distribution and compatibility commitment; the selected target is Huawei HarmonyOS phones/tablets.

## Consequences

HarmonyOS follows native IDE and build conventions while sharing requirements and command behavior with the repository. No Android/iOS build acquires its SDK prerequisite. Contributors selecting `all` will need all three toolchains, and repository guidance must make that change explicit.

The installed macOS Apple-silicon toolchain is the initial supported host baseline. Broader host and older-OS claims need separate evidence. The HarmonyOS app initially has less template behavior than Android/iOS, which remains explicit in platform coverage rather than being hidden behind empty parity checks.

Native build inputs and some app behavior will be platform-specific. Credentials and personal signing settings remain local. Build, lint, IDE import, and runtime launch remain distinct evidence categories.

## Validation

Run shared command tests, real HarmonyOS Debug/Release builds and lint, artifact metadata validation, and native IDE import. Verify prerequisite isolation, failure propagation, aggregate behavior, concurrent platform output separation, and unchanged tracked build inputs. Run Android/iOS verification when their toolchains are available. Attempt emulator/device launch when its prerequisites are available and record its actual result separately.

This proposed decision has no HarmonyOS build or launch result yet. Add the dated verification record when implementation provides that evidence.

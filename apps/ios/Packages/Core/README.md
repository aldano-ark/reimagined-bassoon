# iOS core

Create local Swift packages for deliberately shared capabilities as consumers emerge. Each package states its responsibility, public interface, and dependencies. Core packages cannot depend on features or the app.

Keep SwiftUI and platform APIs out of platform-independent business logic where practical. Use actor isolation and explicit dependency boundaries appropriate to the capability; avoid an unstructured utilities package.

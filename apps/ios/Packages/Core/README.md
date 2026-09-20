# iOS core

The [DesignSystem package](DesignSystem/README.md) supplies native SwiftUI components, content spacing, and debug previews. The app consumes its `DesignSystem` library product; interaction tests run through the app's dedicated UI-test target.

Create local Swift packages for deliberately shared capabilities as consumers emerge. Each package states its responsibility, public interface, and dependencies. Core packages cannot depend on features or the app.

Keep SwiftUI and platform APIs out of platform-independent business logic where practical. Use actor isolation and explicit dependency boundaries appropriate to the capability; avoid an unstructured utilities package.

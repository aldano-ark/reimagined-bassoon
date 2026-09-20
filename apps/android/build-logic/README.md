# Android build conventions

This directory is reserved for an included Gradle build containing convention plugins. Introduce it when multiple modules repeat the same configuration; the initial application needs no custom plugin.

When introduced, register it through `pluginManagement.includeBuild("build-logic")`, keep plugin versions compatible with the checked-in wrapper, and centralize repeated Android/Kotlin configuration here. Product behavior belongs in application or feature code.

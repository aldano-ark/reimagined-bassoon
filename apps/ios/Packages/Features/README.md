# iOS features

Create a local Swift package here when a cohesive product feature exists. Keep its presentation, observable state, feature-specific logic, resources, and tests together. Add its library product to the application's target dependencies.

Features may depend on deliberately shared core packages, but cannot import another feature's implementation or the app. Coordinate cross-feature navigation and dependency construction in `App/`. Keep implementation types internal and publish only the interfaces a consumer needs.

No dummy package or manifest is needed before there is a real feature.

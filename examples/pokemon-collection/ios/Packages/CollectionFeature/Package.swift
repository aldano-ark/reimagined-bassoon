// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "CollectionFeature",
    platforms: [.iOS(.v17)],
    products: [.library(name: "CollectionFeature", targets: ["CollectionFeature"])],
    dependencies: [.package(path: "../../../../../apps/ios/Packages/Core/DesignSystem")],
    targets: [.target(name: "CollectionFeature", dependencies: ["DesignSystem"])]
)

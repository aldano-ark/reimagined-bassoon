pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "PokemonCollection"
include(":app", ":features:collection", ":core:designsystem")
project(":core:designsystem").projectDir = file("../../../apps/android/core/designsystem")

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
}

// Consume the template sources without placing generated output in the template.
project(":core:designsystem") {
    layout.buildDirectory.set(rootProject.layout.projectDirectory.dir("build/designsystem"))
}

import java.net.URI
import java.util.Properties
import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val environmentConfigurations = listOf("dev", "stg", "prod").associateWith { name ->
    val source = rootProject.file("config/$name.properties")
    if (!source.isFile) throw GradleException("Missing environment configuration: $source")
    val values = Properties().apply {
        source.reader(Charsets.UTF_8).use { load(it) }
    }
    fun required(key: String): String {
        val value = values.getProperty(key)
            ?: throw GradleException("$source: missing $key")
        if (value.isBlank() || value != value.trim() || value.any { it.isISOControl() }) {
            throw GradleException("$source: invalid $key")
        }
        return value
    }
    if (required("APP_ENVIRONMENT") != name) {
        throw GradleException("$source: APP_ENVIRONMENT must match $name")
    }
    if (!required("APP_ID").matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))) {
        throw GradleException("$source: invalid APP_ID")
    }
    required("APP_DISPLAY_NAME")
    val raw = required("API_BASE_URL")
    val uri = try { URI(raw) } catch (_: Exception) {
        throw GradleException("$source: invalid API_BASE_URL")
    }
    if (raw.any { it.isWhitespace() } || !uri.isAbsolute ||
        !uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() ||
        uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) {
        throw GradleException("$source: invalid API_BASE_URL")
    }
    values
}
if (environmentConfigurations.values.map { it.getProperty("APP_ID") }.distinct().size != 3) {
    throw GradleException("config/: APP_ID must be distinct for dev, stg, and prod")
}

android {
    namespace = "com.example.nativetemplate"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.example.nativetemplate"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true; buildConfig = true; resValues = true }
    flavorDimensions += "environment"
    productFlavors {
        environmentConfigurations.forEach { (name, values) ->
            create(name) {
                dimension = "environment"
                applicationId = values.getProperty("APP_ID")
                resValue("string", "app_name", values.getProperty("APP_DISPLAY_NAME"))
                for (key in listOf("APP_ENVIRONMENT", "APP_DISPLAY_NAME", "API_BASE_URL")) {
                    val escaped = values.getProperty(key).replace("\\", "\\\\").replace("\"", "\\\"")
                    buildConfigField("String", key, "\"$escaped\"")
                }
            }
        }
    }
    testOptions.unitTests.all {
        it.systemProperty("native.config.dir", rootProject.file("config").absolutePath)
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("release")) {
        (it as HasHostTestsBuilder).hostTests.getValue(HostTestBuilder.UNIT_TEST_TYPE).enable = true
    }
}

dependencies {
    testImplementation(libs.junit)
    implementation(project(":core:designsystem"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
}

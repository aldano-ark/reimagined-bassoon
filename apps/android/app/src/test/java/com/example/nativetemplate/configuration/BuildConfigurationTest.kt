package com.example.nativetemplate.configuration

import com.example.nativetemplate.BuildConfig
import java.io.File
import java.util.Properties
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildConfigurationTest {
    @Test fun compiledConfigurationMatchesSelectedNativeFile() {
        val values = Properties().apply {
            File(System.getProperty("native.config.dir"), BuildConfig.FLAVOR + ".properties")
                .reader(Charsets.UTF_8).use { load(it) }
        }
        val config = AppConfig.fromBuildConfig()
        assertEquals(values.getProperty("APP_ENVIRONMENT"), config.environment.value)
        assertEquals(values.getProperty("APP_ID"), config.applicationId)
        assertEquals(values.getProperty("APP_DISPLAY_NAME"), config.displayName)
        assertEquals(values.getProperty("API_BASE_URL"), config.apiBaseUrl.toString())
        val expectedId = "com.example.nativetemplate" +
            if (BuildConfig.FLAVOR == "prod") "" else "." + BuildConfig.FLAVOR
        assertEquals(expectedId, config.applicationId)
    }
}

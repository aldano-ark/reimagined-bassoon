package com.example.nativetemplate.configuration

import org.junit.Assert.*
import org.junit.Test

class AppConfigTest {
    @Test fun stagingConfigurationPreservesItsValues() {
        val config = AppConfig.parse("stg", "com.example.nativetemplate.stg",
            "NativeTemplate Stg", "https://stg-api.example.com")
        assertEquals(AppEnvironment.STG, config.environment)
        assertEquals("com.example.nativetemplate.stg", config.applicationId)
        assertEquals("NativeTemplate Stg", config.displayName)
        assertEquals("https://stg-api.example.com", config.apiBaseUrl.toString())
    }

    @Test fun invalidEndpointsAreRejectedWithoutFallback() {
        listOf("", " ", "/relative", "https://", "http://api.example.com",
            "https://user:password@api.example.com", "https://api.example.com?q=1",
            "https://api.example.com#fragment", "https://api.example.com/%zz",
            " https://api.example.com", "https://api.example.com/a b").forEach { raw ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                AppConfig.parse("dev", "com.example.nativetemplate.dev", "Dev", raw)
            }
            assertTrue(error.message.orEmpty().contains("API_BASE_URL"))
        }
    }

    @Test fun invalidEnvironmentIdentityAndNameIdentifyTheirKeys() {
        listOf(
            listOf("qa", "app.dev", "Dev", "APP_ENVIRONMENT"),
            listOf(" dev ", "app.dev", "Dev", "APP_ENVIRONMENT"),
            listOf("dev", " ", "Dev", "APP_ID"),
            listOf("dev", "app.dev", " ", "APP_DISPLAY_NAME"),
        ).forEach { (environment, id, name, key) ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                AppConfig.parse(environment, id, name, "https://api.example.com")
            }
            assertTrue(error.message.orEmpty().contains(key))
        }
    }
}

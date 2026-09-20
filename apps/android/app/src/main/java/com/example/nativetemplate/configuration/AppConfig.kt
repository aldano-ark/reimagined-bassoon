package com.example.nativetemplate.configuration

import com.example.nativetemplate.BuildConfig
import java.net.URI
import java.net.URISyntaxException

internal enum class AppEnvironment(val value: String) {
    DEV("dev"), STG("stg"), PROD("prod"),
}

internal class AppConfig private constructor(
    val environment: AppEnvironment,
    val applicationId: String,
    val displayName: String,
    val apiBaseUrl: URI,
) {
    companion object {
        fun fromBuildConfig(): AppConfig = parse(
            BuildConfig.APP_ENVIRONMENT, BuildConfig.APPLICATION_ID,
            BuildConfig.APP_DISPLAY_NAME, BuildConfig.API_BASE_URL,
        )

        fun parse(environment: String, applicationId: String,
                  displayName: String, apiBaseUrl: String): AppConfig {
            fun checked(key: String, value: String): String {
                require(value.isNotBlank() && value == value.trim() &&
                    value.none { it.isISOControl() }) { "Invalid $key" }
                return value
            }
            val selected = AppEnvironment.entries.singleOrNull {
                it.value == checked("APP_ENVIRONMENT", environment)
            }
            requireNotNull(selected) { "Invalid APP_ENVIRONMENT" }
            checked("APP_ID", applicationId)
            checked("APP_DISPLAY_NAME", displayName)
            checked("API_BASE_URL", apiBaseUrl)
            require(apiBaseUrl.none { it.isWhitespace() }) { "Invalid API_BASE_URL" }
            val uri = try { URI(apiBaseUrl) } catch (_: URISyntaxException) {
                throw IllegalArgumentException("Invalid API_BASE_URL")
            }
            require(uri.isAbsolute && uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
                uri.rawQuery == null && uri.rawFragment == null) { "Invalid API_BASE_URL" }
            return AppConfig(selected, applicationId, displayName, uri)
        }
    }
}

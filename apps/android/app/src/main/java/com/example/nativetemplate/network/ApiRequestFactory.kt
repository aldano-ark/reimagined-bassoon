package com.example.nativetemplate.network

import java.net.URI

internal data class ApiRequest(val method: String, val uri: URI)

/** Receives the validated base URL from AppConfig; never performs network I/O. */
internal class ApiRequestFactory(private val baseUrl: URI) {
    fun healthRequest(): ApiRequest = ApiRequest(
        method = "GET",
        uri = URI(baseUrl.toASCIIString().trimEnd('/') + "/health"),
    )
}

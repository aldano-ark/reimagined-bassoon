package com.example.nativetemplate.network

import java.net.URI

internal data class ApiRequest(val method: String, val uri: URI)

/** Receives the validated base URL from AppConfig; never performs network I/O. */
internal class ApiRequestFactory(private val baseUrl: URI) {
    fun healthRequest(): ApiRequest {
        val base = baseUrl.toASCIIString()
        val separator = if (base.endsWith('/')) "" else "/"
        return ApiRequest(method = "GET", uri = URI(base + separator + "health"))
    }
}

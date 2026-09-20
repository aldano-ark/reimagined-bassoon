package com.example.nativetemplate.network

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiRequestFactoryTest {
    @Test fun healthRequestPreservesBasePathAndEncoding() {
        listOf(
            "https://stg-api.example.com" to "https://stg-api.example.com/health",
            "https://stg-api.example.com/" to "https://stg-api.example.com/health",
            "https://api.example.com/v1" to "https://api.example.com/v1/health",
            "https://api.example.com/v1/" to "https://api.example.com/v1/health",
            "https://api.example.com/v1//" to "https://api.example.com/v1//health",
            "https://api.example.com:8443/a%2Fb/" to "https://api.example.com:8443/a%2Fb/health",
        ).forEach { (base, expected) ->
            val request = ApiRequestFactory(URI(base)).healthRequest()
            assertEquals("GET", request.method)
            assertEquals(expected, request.uri.toASCIIString())
        }
    }
}

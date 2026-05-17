package com.rsvpnano.api

import com.rsvpnano.models.NanoBook
import com.rsvpnano.models.NanoBooksResponse
import com.rsvpnano.models.NanoRssFeeds
import com.rsvpnano.models.NanoInfo
import com.rsvpnano.models.NanoSettings
import com.rsvpnano.models.NanoUploadResponse
import com.rsvpnano.models.NanoWifiSettings
import com.rsvpnano.models.NanoWifiUpdate
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

class NanoKtorClient(
    private val httpClient: HttpClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    },
) : NanoClient {
    override suspend fun fetchInfo(baseUrl: String): NanoInfo =
        json.decodeFromString(NanoInfo.serializer(), requestText(baseUrl, "api/info"))

    override suspend fun listBooks(baseUrl: String): List<NanoBook> {
        val response = requestText(baseUrl, "api/books")
        val wrapper = json.decodeFromString(NanoBooksResponse.serializer(), response)
        return wrapper.books
    }

    override suspend fun fetchSettings(baseUrl: String): NanoSettings =
        json.decodeFromString(NanoSettings.serializer(), requestText(baseUrl, "api/settings"))

    override suspend fun updateSettings(baseUrl: String, settings: NanoSettings): NanoSettings {
        val response = httpClient.patch(buildUrl(baseUrl, "api/settings")) {
            contentType(ContentType.Application.Json)
            setBody(settings)
        }
        val body = response.body<String>()
        return decodeDeviceResponse(response.status, body, NanoSettings.serializer())
    }

    override suspend fun fetchWifiSettings(baseUrl: String): NanoWifiSettings =
        json.decodeFromString(NanoWifiSettings.serializer(), requestText(baseUrl, "api/wifi"))

    override suspend fun updateWifi(baseUrl: String, ssid: String, password: String): NanoWifiSettings {
        val response = httpClient.put(buildUrl(baseUrl, "api/wifi")) {
            contentType(ContentType.Application.Json)
            setBody(NanoWifiUpdate(ssid = ssid, password = password))
        }
        val body = response.body<String>()
        return decodeDeviceResponse(response.status, body, NanoWifiSettings.serializer())
    }

    override suspend fun forgetWifi(baseUrl: String): NanoWifiSettings {
        val response = httpClient.delete(buildUrl(baseUrl, "api/wifi"))
        val body = response.body<String>()
        return decodeDeviceResponse(response.status, body, NanoWifiSettings.serializer())
    }

    override suspend fun fetchRssFeeds(baseUrl: String): NanoRssFeeds =
        json.decodeFromString(NanoRssFeeds.serializer(), requestText(baseUrl, "api/rss-feeds"))

    override suspend fun updateRssFeeds(baseUrl: String, feeds: List<String>): NanoRssFeeds {
        val response = httpClient.put(buildUrl(baseUrl, "api/rss-feeds")) {
            contentType(ContentType.Application.Json)
            setBody(NanoRssFeeds(ok = true, feeds = feeds))
        }
        val body = response.body<String>()
        return decodeDeviceResponse(response.status, body, NanoRssFeeds.serializer())
    }

    override suspend fun uploadBook(baseUrl: String, name: String, data: ByteArray, category: String?): NanoBook {
        val response = httpClient.post(buildUrl(baseUrl, "api/books")) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", data, headers = io.ktor.http.Headers.build {
                            append("Content-Disposition", "filename=\"$name\"")
                            append("Content-Type", ContentType.Application.OctetStream.toString())
                        })
                        if (!category.isNullOrBlank()) {
                            append("category", category)
                        }
                        append("name", name)
                    }
                )
            )
        }

        ensureSuccess(response.status)
        val body = response.body<String>()
        return json.decodeFromString(NanoBook.serializer(), body)
    }

    override suspend fun deleteBook(baseUrl: String, name: String): NanoUploadResponse {
        val response = httpClient.delete(buildUrl(baseUrl, "api/books?name=$name"))
        val body = response.body<String>()
        return decodeDeviceResponse(response.status, body, NanoUploadResponse.serializer())
    }

    private suspend fun requestText(baseUrl: String, path: String): String {
        val response = httpClient.get(buildUrl(baseUrl, path))
        ensureSuccess(response.status)
        return response.body<String>()
    }

    private fun buildUrl(baseUrl: String, path: String) = URLBuilder(baseUrl).apply {
        appendPathSegments(path.split('/').filter { it.isNotBlank() })
    }.build()

    private fun ensureSuccess(status: HttpStatusCode) {
        if (!status.isSuccess()) {
            throw NanoClientError("Device rejected request with HTTP $status")
        }
    }

    private inline fun <reified T> decodeDeviceResponse(
        status: HttpStatusCode,
        body: String,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T {
        if (!status.isSuccess()) {
            val decoded = runCatching { json.decodeFromString(NanoUploadResponse.serializer(), body) }.getOrNull()
            throw NanoClientError(decoded?.error ?: "Device rejected request with HTTP $status")
        }
        return json.decodeFromString(serializer, body)
    }

    @Serializable
    private data class BookListResponse(
        val books: List<NanoBook>,
    )
}

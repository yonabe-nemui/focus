package app.focus.personal.network

import app.focus.personal.model.BlueskyFeedViewPost
import app.focus.personal.model.BlueskySearchResponse
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.BlueskyTimelineResponse
import app.focus.personal.model.MutedWord
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BlueskyClient(private val client: HttpClient) {
    private val baseUrl = "https://bsky.social/xrpc"
    private val publicApiUrl = "https://public.api.bsky.app/xrpc"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createSession(
        handle: String, 
        appPassword: String, 
        authCode: String? = null
    ): BlueskySession {
        val response: HttpResponse = client.post("$baseUrl/com.atproto.server.createSession") {
            contentType(ContentType.Application.Json)
            val bodyMap = mutableMapOf("identifier" to handle, "password" to appPassword)
            if (authCode != null) {
                bodyMap["authFactorToken"] = authCode
            }
            setBody(bodyMap)
        }

        // レスポンスボディは accessJwt/refreshJwt を含むためログに出さない
        val responseBody = response.body<String>()
        Napier.d("BlueSky createSession status: ${response.status}")

        if (response.status == HttpStatusCode.OK) {
            return json.decodeFromString<BlueskySession>(responseBody)
        } else if (response.status == HttpStatusCode.Unauthorized) {
            val is2fa = responseBody.contains("AuthFactor", ignoreCase = true) ||
                    responseBody.contains("sign in code", ignoreCase = true) ||
                    responseBody.contains("sign on code", ignoreCase = true)
            if (is2fa && authCode == null) {
                throw Exception("AuthFactorRequired")
            } else if (is2fa && authCode != null) {
                throw Exception("AuthFactorInvalid")
            } else {
                throw Exception("Unauthorized: $responseBody")
            }
        } else {
            throw Exception("HTTP ${response.status}: $responseBody")
        }
    }

    suspend fun refreshSession(refreshJwt: String): BlueskySession {
        val response: HttpResponse = client.post("$baseUrl/com.atproto.server.refreshSession") {
            header("Authorization", "Bearer $refreshJwt")
        }

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            val errorBody = response.body<String>()
            throw Exception("Session refresh failed: $errorBody")
        }
    }

    suspend fun getMutedWords(session: BlueskySession): List<MutedWord> {
        val response = client.get("$baseUrl/app.bsky.actor.getPreferences") {
            header("Authorization", "Bearer ${session.accessJwt}")
        }
        val prefsString = response.body<String>()
        val prefsJson = json.parseToJsonElement(prefsString).jsonObject
        val prefsArray = prefsJson["preferences"]?.jsonArray ?: return emptyList()
        
        for (pref in prefsArray) {
            val obj = pref.jsonObject
            if (obj["\$type"]?.jsonPrimitive?.content == "app.bsky.actor.defs#mutedWordsPref") {
                val words = obj["items"]?.jsonArray ?: continue
                return words.map { 
                    val wordObj = it.jsonObject
                    MutedWord(
                        value = wordObj["value"]?.jsonPrimitive?.content ?: "",
                        targets = wordObj["targets"]?.jsonArray?.map { t -> t.jsonPrimitive.content } ?: listOf("content")
                    )
                }
            }
        }
        return emptyList()
    }

    suspend fun getTimeline(session: BlueskySession, limit: Int = 50, cursor: String? = null): BlueskyTimelineResponse {
        val response = client.get("$baseUrl/app.bsky.feed.getTimeline") {
            header("Authorization", "Bearer ${session.accessJwt}")
            parameter("limit", limit)
            if (cursor != null) parameter("cursor", cursor)
        }
        Napier.d("getTimeline: status=${response.status}")
        if (response.status != HttpStatusCode.OK) {
            val errorBody = response.body<String>()
            Napier.e("getTimeline failed [${response.status}]: $errorBody")
            throw Exception("getTimeline failed [${response.status}]: $errorBody")
        }
        val result = response.body<BlueskyTimelineResponse>()
        Napier.d("getTimeline: got ${result.feed.size} posts")
        return result
    }

    suspend fun searchPosts(
        query: String,
        sort: String = "latest",
        session: BlueskySession? = null,
        cursor: String? = null
    ): BlueskySearchResponse {
        val url = if (session != null) baseUrl else publicApiUrl
        val response = client.get("$url/app.bsky.feed.searchPosts") {
            parameter("q", query)
            parameter("sort", sort)
            if (session != null) {
                header("Authorization", "Bearer ${session.accessJwt}")
            }
            if (cursor != null) parameter("cursor", cursor)
        }
        if (response.status != HttpStatusCode.OK) {
            val errorBody = response.body<String>()
            throw Exception("searchPosts failed [${response.status}]: $errorBody")
        }
        return response.body()
    }
}

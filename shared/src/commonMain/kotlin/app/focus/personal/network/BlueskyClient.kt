package app.focus.personal.network

import app.focus.personal.model.BlueskyPost
import app.focus.personal.model.BlueskyRecord
import app.focus.personal.model.BlueskySearchResponse
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MutedWord
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
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
                bodyMap["authCode"] = authCode
            }
            setBody(bodyMap)
        }

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else if (response.status == HttpStatusCode.Unauthorized) {
            val errorBody = response.body<String>()
            val is2fa = errorBody.contains("AuthFactor", ignoreCase = true) || 
                       errorBody.contains("sign in code", ignoreCase = true) ||
                       errorBody.contains("sign on code", ignoreCase = true)
            
            if (is2fa) {
                throw Exception("AuthFactorRequired")
            } else {
                throw Exception("Unauthorized: $errorBody")
            }
        } else {
            val errorBody = response.body<String>()
            throw Exception("HTTP ${response.status}: $errorBody")
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

    suspend fun searchPosts(
        query: String, 
        lang: String = "ja", 
        sort: String = "top",
        session: BlueskySession? = null
    ): BlueskySearchResponse {
        val url = if (session != null) baseUrl else publicApiUrl
        val response = client.get("$url/app.bsky.feed.searchPosts") {
            parameter("q", query)
            parameter("lang", lang)
            parameter("sort", sort)
            if (session != null) {
                header("Authorization", "Bearer ${session.accessJwt}")
            }
        }
        return response.body()
    }
}

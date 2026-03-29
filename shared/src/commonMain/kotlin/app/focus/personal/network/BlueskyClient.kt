package app.focus.personal.network

import app.focus.personal.model.BlueskyPost
import app.focus.personal.model.BlueskyRecord
import app.focus.personal.model.BlueskySearchResponse
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MutedWord
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BlueskyClient(private val client: HttpClient) {
    private val baseUrl = "https://bsky.social/xrpc"
    private val publicApiUrl = "https://public.api.bsky.app/xrpc"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createSession(handle: String, appPassword: String): BlueskySession {
        val response = client.post("$baseUrl/com.atproto.server.createSession") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("identifier" to handle, "password" to appPassword))
        }
        return response.body()
    }

    suspend fun getMutedWords(session: BlueskySession): List<MutedWord> {
        val response = client.get("$baseUrl/app.bsky.actor.getPreferences") {
            header("Authorization", "Bearer ${session.accessJwt}")
        }
        val prefsString = response.body<String>()
        val prefsJson = json.parseToJsonElement(prefsString).jsonObject
        val prefsArray = prefsJson["preferences"]?.jsonArray ?: return emptyList()
        
        // Find 'app.bsky.actor.defs#contentLabelPref' or 'mutedWords' if available
        // Simplified: looking for mutedWords in preferences
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

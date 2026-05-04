package app.focus.personal.network

import app.focus.personal.model.MisskeyNote
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MisskeyClient(private val client: HttpClient) {

    suspend fun searchNotes(
        instanceUrl: String,
        query: String,
        limit: Int = 20,
        token: String? = null,
        untilId: String? = null
    ): List<MisskeyNote> {
        val requestBody = buildJsonObject {
            put("query", query)
            put("limit", limit)
            if (token != null) put("i", token)
            if (untilId != null) put("untilId", untilId)
        }
        Napier.d("Misskey searchNotes: https://$instanceUrl/api/notes/search query=$query untilId=$untilId")
        return client.post("https://$instanceUrl/api/notes/search") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()
    }

    suspend fun getHomeTimeline(
        instanceUrl: String,
        token: String,
        limit: Int = 20,
        untilId: String? = null
    ): List<MisskeyNote> {
        val requestBody = buildJsonObject {
            put("limit", limit)
            put("i", token)
            if (untilId != null) put("untilId", untilId)
        }
        Napier.d("Misskey getHomeTimeline: https://$instanceUrl/api/notes/timeline untilId=$untilId")
        return client.post("https://$instanceUrl/api/notes/timeline") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()
    }
}
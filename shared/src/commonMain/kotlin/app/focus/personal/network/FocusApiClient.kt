package app.focus.personal.network

import app.focus.personal.model.PagedFeedResponse
import app.focus.personal.model.RssItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

// accessJwt が null の場合は未ログイン（サーバー側はセッションなしで検索のみ行う）
@Serializable
data class BlueSkyFeedRequest(val accessJwt: String?, val query: String, val cursor: String? = null)

@Serializable
data class MisskeyFeedRequest(val instanceUrl: String, val apiToken: String?, val query: String, val untilId: String? = null)

class FocusApiClient(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun fetchGoogleFeed(): List<RssItem> =
        client.get("$baseUrl/api/feed/google").body()

    suspend fun fetchHatenaFeed(): List<RssItem> =
        client.get("$baseUrl/api/feed/hatena").body()

    suspend fun fetchBlueskyPage(accessJwt: String?, query: String, cursor: String?): PagedFeedResponse {
        val response: HttpResponse = client.post("$baseUrl/api/feed/bluesky") {
            contentType(ContentType.Application.Json)
            setBody(BlueSkyFeedRequest(accessJwt = accessJwt, query = query, cursor = cursor))
        }
        if (!response.status.isSuccess()) {
            throw Exception("BlueSky feed error: ${response.status}")
        }
        return response.body()
    }

    suspend fun fetchMisskeyPage(instanceUrl: String, apiToken: String?, query: String, untilId: String?): List<RssItem> {
        val response: HttpResponse = client.post("$baseUrl/api/feed/misskey") {
            contentType(ContentType.Application.Json)
            setBody(MisskeyFeedRequest(instanceUrl = instanceUrl, apiToken = apiToken, query = query, untilId = untilId))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Misskey feed error: ${response.status}")
        }
        return response.body()
    }

}

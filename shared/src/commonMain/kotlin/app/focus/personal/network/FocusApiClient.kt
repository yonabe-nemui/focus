package app.focus.personal.network

import app.focus.personal.model.RssItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class FocusApiClient(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun fetchGoogleFeed(): List<RssItem> =
        client.get("$baseUrl/api/feed/google").body()

    suspend fun fetchHatenaFeed(): List<RssItem> =
        client.get("$baseUrl/api/feed/hatena").body()
}

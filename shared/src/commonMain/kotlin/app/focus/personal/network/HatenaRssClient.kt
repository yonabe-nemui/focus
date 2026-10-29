package app.focus.personal.network

import app.focus.personal.model.HatenaRdf
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import nl.adaptivity.xmlutil.serialization.XML

class HatenaRssClient(private val client: HttpClient) {
    // XML.v1 は autoPolymorphic が既定で有効
    private val xml = XML.v1 {
        policy {
            ignoreUnknownChildren()
        }
    }

    suspend fun fetchHotEntry(): HatenaRdf {
        val url = "https://b.hatena.ne.jp/hotentry.rss"
        return fetchAndParse(url)
    }

    suspend fun fetchEntryList(): HatenaRdf {
        val url = "https://b.hatena.ne.jp/entrylist.rss"
        return fetchAndParse(url)
    }

    suspend fun fetchItHotEntry(): HatenaRdf {
        val url = "https://b.hatena.ne.jp/hotentry/it.rss"
        return fetchAndParse(url)
    }

    private suspend fun fetchAndParse(url: String): HatenaRdf {
        val response = client.get(url)
        val body = response.bodyAsText()
        return xml.decodeFromString(HatenaRdf.serializer(), body)
    }
}

package app.focus.personal.network

import app.focus.personal.model.HatenaRdf
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy

class HatenaRssClient(private val client: HttpClient) {
    private val xml = XML {
        autoPolymorphic = true
        policy = DefaultXmlSerializationPolicy {
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

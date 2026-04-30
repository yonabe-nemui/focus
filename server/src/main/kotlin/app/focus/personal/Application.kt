package app.focus.personal

import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.repository.RssRepository
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }

    val httpClient = HttpClient(OkHttp) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val repository = RssRepository(
        database = null,
        googleApi = GoogleRssClient(httpClient),
        hatenaApi = HatenaRssClient(httpClient),
        blueskyApi = BlueskyClient(httpClient)
    )

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        route("/api") {
            get("/feed/google") {
                val items = repository.fetchAllGoogleTopics()
                call.respond(items)
            }
            get("/feed/hatena") {
                val items = repository.fetchAllHatenaEntries()
                call.respond(items)
            }
        }
    }
}

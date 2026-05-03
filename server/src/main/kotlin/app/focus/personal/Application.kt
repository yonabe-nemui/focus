package app.focus.personal

import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.BlueSkyFeedRequest
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.network.MisskeyClient
import app.focus.personal.network.MisskeyFeedRequest
import app.focus.personal.repository.RssRepository
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
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
        blueskyApi = BlueskyClient(httpClient),
        misskeyApi = MisskeyClient(httpClient)
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
            post("/feed/bluesky") {
                val request = call.receive<BlueSkyFeedRequest>()
                val session = if (request.accessJwt.isNotEmpty()) {
                    BlueskySession(accessJwt = request.accessJwt, refreshJwt = "", handle = "", did = "")
                } else null
                val items = repository.fetchBlueskyEntries(request.query, session)
                call.respond(items)
            }
            post("/feed/misskey") {
                val request = call.receive<MisskeyFeedRequest>()
                val settings = MisskeySettings(instanceUrl = request.instanceUrl, apiToken = request.apiToken)
                val items = repository.fetchMisskeyEntries(request.query, settings)
                call.respond(items)
            }
        }
    }
}

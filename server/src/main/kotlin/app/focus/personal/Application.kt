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
import io.ktor.http.HttpStatusCode
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

// ミュートワードのフィルタはクライアント側（MuteWordStore）で行うため、サーバーは素通しで返す。

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
                call.respond(repository.fetchAllGoogleTopics())
            }
            get("/feed/hatena") {
                call.respond(repository.fetchAllHatenaEntries())
            }
            post("/feed/bluesky") {
                try {
                    val request = call.receive<BlueSkyFeedRequest>()
                    val session = request.accessJwt?.takeIf { it.isNotEmpty() }?.let {
                        BlueskySession(accessJwt = it, refreshJwt = "", handle = "", did = "")
                    }
                    val pagedResult = repository.fetchBlueskyPage(request.query, session, request.cursor)
                    call.respond(pagedResult)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "error")))
                }
            }
            post("/feed/misskey") {
                try {
                    val request = call.receive<MisskeyFeedRequest>()
                    val settings = MisskeySettings(instanceUrl = request.instanceUrl, apiToken = request.apiToken)
                    val items = repository.fetchMisskeyPage(request.query, settings, request.untilId)
                    call.respond(items)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "error")))
                }
            }
        }
    }
}

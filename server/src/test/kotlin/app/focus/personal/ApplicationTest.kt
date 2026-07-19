package app.focus.personal

import app.focus.personal.network.FOCUS_API_KEY_HEADER
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ApplicationTest {

    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            module()
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testApiRejectedWithoutApiKey() = testApplication {
        application {
            module(apiKey = "secret")
        }
        val response = client.get("/api/feed/google")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testApiRejectedWithWrongApiKey() = testApplication {
        application {
            module(apiKey = "secret")
        }
        val response = client.get("/api/feed/google") {
            header(FOCUS_API_KEY_HEADER, "wrong")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testHealthNotProtectedByApiKey() = testApplication {
        application {
            module(apiKey = "secret")
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testApiPassesAuthWithCorrectApiKey() = testApplication {
        application {
            module(apiKey = "secret")
        }
        // 実際のフィード取得は外部依存のため、認証を通過すること(401 でないこと)のみ確認する
        val response = client.get("/api/feed/google") {
            header(FOCUS_API_KEY_HEADER, "secret")
        }
        assertNotEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

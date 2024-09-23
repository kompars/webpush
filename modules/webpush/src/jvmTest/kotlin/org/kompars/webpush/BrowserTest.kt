package org.kompars.webpush

import com.microsoft.playwright.*
import com.microsoft.playwright.assertions.*
import com.microsoft.playwright.assertions.PlaywrightAssertions.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.nio.file.*
import kotlin.io.path.*
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.html.*

@OptIn(ExperimentalPathApi::class)
class BrowserTest {
    @Test
    fun shouldReceiveNotification() {
        runBlocking {
            val webPush = WebPush(
                subject = "mailto:github@kompars.org",
                publicKey = "BJwwFRoDoOx2vQPfvbeo-m1fZZHo6lIjtyTlWHjLNSCtHuWdGryZD5xt0LeawVQq7G60ioID1sC33fEoQT8jCzg",
                privateKey = "P5GjTLppISlmUyNiZqZi0HNq7GXFniAdcBECNsKBxfI",
            )

            val payload = "Test"

            val server = embeddedServer(CIO, port = 0) {
                routing {
                    staticResources("/", null)

                    get("/") {
                        call.respondHtml {
                            head { script(type = "module", src = "test.js") {} }
                            body {}
                        }
                    }

                    get("/vapid") {
                        call.respondBytes(webPush.applicationServerKey)
                    }

                    post("/send") {
                        val subscription = Subscription.fromJson(call.receiveText())
                        webPush.send(subscription, payload)

                        call.respondText("OK")
                    }
                }
            }

            startServer(server) { port ->
                openPage("http://127.0.0.1:$port") { page ->
                    assertThat(page.locator("body")).hasText(
                        payload,
                        LocatorAssertions.HasTextOptions().setTimeout(60_000.0),
                    )
                }
            }
        }
    }

    private fun openPage(url: String, block: (Page) -> Unit) {
        val tempDir = Files.createTempDirectory("test")

        val contextOptions = BrowserType.LaunchPersistentContextOptions()
            .setHeadless(false)
            .setChromiumSandbox(false)
            .setIgnoreHTTPSErrors(true)
            .setPermissions(listOf("notifications"))

        try {
            Playwright.create().use { playwright ->
                playwright.chromium().launchPersistentContext(tempDir, contextOptions).use { context ->
                    context.newPage().use { page ->
                        page.navigate(url)
                        block(page)
                    }
                }
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private suspend fun startServer(server: EmbeddedServer<*, *>, block: (Int) -> Unit) {
        try {
            server.start()
            block(server.engine.resolvedConnectors().first().port)
        } finally {
            server.stop()
        }
    }
}

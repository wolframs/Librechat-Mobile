package com.garfiec.librechat.core.network.api

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Test

class MarketApiTest {
    @Test fun quoteUsesServerSidecarAndPreservesUnavailableRatesAndReferenceSource() = runTest {
        val model = "vendor/model + preview"
        val engine = MockEngine { request ->
            assertThat(request.url.encodedPath).isEqualTo("/cost/markets")
            assertThat(request.url.parameters["model"]).isEqualTo(model)
            respond(
                """{"model":"vendor/model + preview","best":{"input":0.0,"output":null},
                    "direct":{"input":5,"output":25,"source":"provider","marketplaceInput":6},
                    "healthySellers":2,"fetchedAt":"2026-09-05T17:42:50+00:00","sellers":[]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }.use { client ->
            val price = MarketApi(client).price(model)
            assertThat(price.best.input).isEqualTo(0.0)
            assertThat(price.best.output).isNull()
            assertThat(price.direct.source).isEqualTo("provider")
        }
    }

    @Test fun renamedEndpointsAreDiscoveredWithoutAHardcodedSurplusName() = runTest {
        val engine = MockEngine { request ->
            assertThat(request.url.encodedPath).isEqualTo("/cost/markets/endpoints")
            respond("""{"endpoints":["My marketplace","Claude via market"]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        HttpClient(engine) { install(ContentNegotiation) { json() } }.use { client ->
            assertThat(MarketApi(client).endpoints().endpoints)
                .containsExactly("My marketplace", "Claude via market")
        }
    }
}

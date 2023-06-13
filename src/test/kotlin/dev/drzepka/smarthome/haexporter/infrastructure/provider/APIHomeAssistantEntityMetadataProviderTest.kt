package dev.drzepka.smarthome.haexporter.infrastructure.provider

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.marcinziolo.kotlin.wiremock.EqualTo
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.returnsJson
import dev.drzepka.smarthome.haexporter.application.model.EntityMetadata
import dev.drzepka.smarthome.haexporter.infrasctucture.properties.HomeAssistantAPIProperties
import dev.drzepka.smarthome.haexporter.infrasctucture.provider.APIHomeAssistantEntityMetadataProvider
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.collections.set

@WireMockTest
class APIHomeAssistantEntityMetadataProviderTest {

    private val authToken = "token123"
    private lateinit var wireMock: WireMock
    private lateinit var provider: APIHomeAssistantEntityMetadataProvider

    @BeforeEach
    fun beforeEach(wm: WireMockRuntimeInfo) {
        wireMock = wm.wireMock

        val properties = HomeAssistantAPIProperties(wm.httpBaseUrl, authToken)
        provider = APIHomeAssistantEntityMetadataProvider(properties)
    }

    @Test
    fun `should get entity metadata from Home Assistant API`() = runBlocking {
        wireMock.get {
            urlPath equalTo "/api/states"
            headers[HttpHeaders.Authorization] = EqualTo("Bearer $authToken")
        } returnsJson {
            body = """
                [
                    {
                        "attributes": {},
                        "entity_id": "sun.sun",
                        "last_changed": "2023-06-13T12:01:32.418320+00:00",
                        "state": "below_horizon"
                    },
                    {
                        "attributes": {},
                        "entity_id": "process.Dropbox",
                        "last_changed": "2023-06-13T12:42:32.698571+01:00",
                        "state": "on"
                    }
                ]
              """
        }

        val metadata = provider.getEntityMetadata()

        val firstTime = OffsetDateTime.of(2023, 6, 13, 12, 1, 32, 418320000, ZoneOffset.UTC).toInstant()
        val secondTime = OffsetDateTime.of(2023, 6, 13, 12,42,32,698571000, ZoneOffset.ofHours(1)).toInstant()

        then(metadata).hasSize(2)
        then(metadata).element(0).isEqualTo(EntityMetadata("sun.sun", firstTime))
        then(metadata).element(1).isEqualTo(EntityMetadata("process.Dropbox", secondTime))

        Unit
    }
}

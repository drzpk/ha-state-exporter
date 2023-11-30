package dev.drzepka.smarthome.haexporter.infrastructure.provider

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.drzepka.smarthome.haexporter.application.model.EntityMetadata
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantEntityMetadataProvider
import dev.drzepka.smarthome.haexporter.infrastructure.properties.HomeAssistantAPIProperties
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import java.time.OffsetDateTime

class APIHomeAssistantEntityMetadataProvider(private val properties: HomeAssistantAPIProperties) :
    HomeAssistantEntityMetadataProvider {

    private val client = HttpClient(Apache5) {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
        defaultRequest {
            header(HttpHeaders.Authorization, "Bearer ${properties.token}")
        }
    }

    override suspend fun getEntityMetadata(): List<EntityMetadata> = client
        .get("${properties.url}/api/states").body<List<HAState>>()
        .map { it.toEntityMetadata() }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    private data class HAState(
        val entityId: String,
        val lastChanged: String,
        val lastUpdated: String
    ) {
        fun toEntityMetadata(): EntityMetadata {
            val lastChangedTs = OffsetDateTime.parse(lastChanged).toInstant()
            val lastUpdatedTs = OffsetDateTime.parse(lastUpdated).toInstant()

            // lastChanged - updated when state is changed
            // lastUpdated - updated when state or attribute is changed
            // State records in DB are created for state or attribute change, so the highest value must be used
            // todo: move this logic to one of the higher layers
            return EntityMetadata(entityId, maxOf(lastChangedTs, lastUpdatedTs))
        }
    }
}

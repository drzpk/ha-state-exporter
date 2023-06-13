package dev.drzepka.smarthome.haexporter.infrasctucture.provider

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.drzepka.smarthome.haexporter.application.model.EntityMetadata
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantEntityMetadataProvider
import dev.drzepka.smarthome.haexporter.infrasctucture.properties.HomeAssistantAPIProperties
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
        .map { EntityMetadata(it.entityId, OffsetDateTime.parse(it.lastChanged).toInstant()) }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    private data class HAState(
        val entityId: String,
        val lastChanged: String
    )
}

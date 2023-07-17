package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.application.DuplicatedEntitySelectorsException
import dev.drzepka.smarthome.haexporter.domain.properties.EntitiesProperties
import dev.drzepka.smarthome.haexporter.domain.properties.EntityProperties
import dev.drzepka.smarthome.haexporter.domain.value.ANY_VALUE
import dev.drzepka.smarthome.haexporter.domain.value.ElementalEntitySelector
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import dev.drzepka.smarthome.haexporter.domain.value.EntitySelector
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class EntityConfigurationResolverTest {

    @Test
    fun `should resolve configuration for existing entity`() {
        val properties = EntitiesProperties(
            EntityProperties(EntitySelector("class1", "dev1", "sensor"), "mapping1"),
            EntityProperties(EntitySelector(ANY_VALUE, "dev2", "sensor"), "mapping2"),
            EntityProperties(EntitySelector("class3", "dev3", ANY_VALUE), "mapping3"),
            EntityProperties(EntitySelector(ANY_VALUE, "dev4", ANY_VALUE), "mapping4"),
            EntityProperties(EntitySelector("class5", "dev5", ANY_VALUE), "mapping5")
        )

        val resolver = EntityConfigurationResolver(properties)

        then(resolver.resolve(EntityId("class1", "dev1", "sensor"))).matches { it?.mapping == "mapping1" }
        then(resolver.resolve(EntityId("any-class", "dev2", "sensor"))).matches { it?.mapping == "mapping2" }
        then(resolver.resolve(EntityId("class3", "dev3", "any-sensor"))).matches { it?.mapping == "mapping3" }
        then(resolver.resolve(EntityId("any-class", "dev4", "any-sensor"))).matches { it?.mapping == "mapping4" }
        then(resolver.resolve(EntityId("class5", "dev5", "any-sensor"))).matches { it?.mapping == "mapping5" }

        then(resolver.resolve(EntityId("class1", "dev1", "sensor00"))).isNull()
        then(resolver.resolve(EntityId("class1", "dev1xx", "sensor"))).isNull()
        then(resolver.resolve(EntityId("class_", "dev1", "sensor"))).isNull()
    }

    @Test
    fun `should resolve first matching entity configuration`() {
        val properties = EntitiesProperties(
            EntityProperties(EntitySelector("class1", "dev1", "sensor"), "mapping1"),
            EntityProperties(EntitySelector("class2", "dev1", "sensor"), "mapping2"),
            EntityProperties(EntitySelector("class1", "dev1", ANY_VALUE), "mapping3"),
            EntityProperties(EntitySelector(ANY_VALUE, "dev1", ANY_VALUE), "mapping4")
        )

        val resolver = EntityConfigurationResolver(properties)

        then(resolver.resolve(EntityId("class1", "dev1", "sensor"))).matches { it?.mapping == "mapping1" }
        then(resolver.resolve(EntityId("class2", "dev1", "sensor"))).matches { it?.mapping == "mapping2" }
        then(resolver.resolve(EntityId("class1", "dev1", "another"))).matches { it?.mapping == "mapping3" }
        then(resolver.resolve(EntityId("unknown", "dev1", "another"))).matches { it?.mapping == "mapping4" }
    }

    @Test
    fun `should resolve configuration with multiple values in selector`() {
        val properties = EntitiesProperties(
            EntityProperties(EntitySelector(listOf("class1"), "dev1", listOf("sensor1", "sensor2")), "mapping1"),
            EntityProperties(EntitySelector(listOf("class2", "class3"), "dev1", listOf("sensor3")), "mapping2")
        )

        val resolver = EntityConfigurationResolver(properties)

        then(resolver.resolve(EntityId("class1", "dev1", "sensor1"))).matches { it?.mapping == "mapping1" }
        then(resolver.resolve(EntityId("class1", "dev1", "sensor2"))).matches { it?.mapping == "mapping1" }
        then(resolver.resolve(EntityId("class2", "dev1", "sensor3"))).matches { it?.mapping == "mapping2" }
        then(resolver.resolve(EntityId("class3", "dev1", "sensor3"))).matches { it?.mapping == "mapping2" }
        then(resolver.resolve(EntityId("class1", "dev1", "sensor3"))).isNull()
        then(resolver.resolve(EntityId("class2", "dev1", "sensor4"))).isNull()
    }

    @ParameterizedTest
    @MethodSource("getEntityPropertiesWithDuplicates")
    fun `should throw exception on duplicated properties`(source: Pair<ElementalEntitySelector, List<EntityProperties>>) {
        val properties = EntitiesProperties(*source.second.toTypedArray())

        val result = kotlin.runCatching {
            EntityConfigurationResolver(properties)
        }

        then(result.isFailure).isTrue

        val exception = result.exceptionOrNull()
        then(exception).isInstanceOf(DuplicatedEntitySelectorsException::class.java)

        exception as DuplicatedEntitySelectorsException
        then(exception.duplicates).hasSize(1)
        then(exception.duplicates.first()).isEqualTo(source.first)
    }

    companion object {

        @JvmStatic
        private fun getEntityPropertiesWithDuplicates(): Stream<Pair<ElementalEntitySelector, List<EntityProperties>>> =
            listOf(
                ElementalEntitySelector(ANY_VALUE, "dev1", ANY_VALUE) to listOf(
                    EntityProperties(EntitySelector(ANY_VALUE, "dev1", ANY_VALUE), "duplicated-dev"),
                    EntityProperties(EntitySelector(ANY_VALUE, "dev1", ANY_VALUE), "duplicated-dev"),
                    EntityProperties(EntitySelector(ANY_VALUE, "dev1", "sensor"), "other"),
                ),
                ElementalEntitySelector("class1", "dev1", ANY_VALUE) to listOf(
                    EntityProperties(EntitySelector("class1", "dev1", ANY_VALUE), "duplicated-class-and-dev"),
                    EntityProperties(EntitySelector("class1", "dev1", ANY_VALUE), "duplicated-class-and-dev"),
                    EntityProperties(EntitySelector("class1", "dev1", "sensor"), "other")
                ),
                ElementalEntitySelector("class1", "dev1", "sensor") to listOf(
                    EntityProperties(EntitySelector("class1", "dev1", "sensor"), "duplicated-everything"),
                    EntityProperties(EntitySelector("class1", "dev1", "sensor"), "duplicated-everything"),
                    EntityProperties(EntitySelector("class1", "dev1", "other"), "other")
                ),
                ElementalEntitySelector("class1", "dev1", "sensor2") to listOf(
                    EntityProperties(
                        EntitySelector(listOf("class1"), "dev1", listOf("sensor1", "sensor2")),
                        "overlapping-sensor"
                    ),
                    EntityProperties(
                        EntitySelector(listOf("class1"), "dev1", listOf("sensor2", "sensor3")),
                        "overlapping-sensor"
                    )
                )
            ).stream()
    }
}

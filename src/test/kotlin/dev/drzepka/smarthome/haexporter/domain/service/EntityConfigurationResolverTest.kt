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
            EntityProperties(EntitySelector("domain1", "dev1", "suffix"), "mapping1"),
            EntityProperties(EntitySelector(ANY_VALUE, "dev2", "suffix"), "mapping2"),
            EntityProperties(EntitySelector("domain3", "dev3", ANY_VALUE), "mapping3"),
            EntityProperties(EntitySelector(ANY_VALUE, "dev4", ANY_VALUE), "mapping4"),
            EntityProperties(EntitySelector("domain5", "dev5", ANY_VALUE), "mapping5")
        )

        val resolver = EntityConfigurationResolver(properties)

        then(resolver.resolve(EntityId("domain1", "dev1", "suffix"))).matches { it?.mapping == "mapping1" }
        then(resolver.resolve(EntityId("any-domain", "dev2", "suffix"))).matches { it?.mapping == "mapping2" }
        then(resolver.resolve(EntityId("domain3", "dev3", "any-suffix"))).matches { it?.mapping == "mapping3" }
        then(resolver.resolve(EntityId("any-domain", "dev4", "any-suffix"))).matches { it?.mapping == "mapping4" }
        then(resolver.resolve(EntityId("domain5", "dev5", "any-suffix"))).matches { it?.mapping == "mapping5" }

        then(resolver.resolve(EntityId("domain1", "dev1", "suffix00"))).isNull()
        then(resolver.resolve(EntityId("domain1", "dev1xx", "suffix"))).isNull()
        then(resolver.resolve(EntityId("domain_", "dev1", "suffix"))).isNull()
    }

    @Test
    fun `should resolve first matching entity configuration`() {
        val properties = EntitiesProperties(
            EntityProperties(EntitySelector("domain1", "dev1", "suffix"), "mapping1"),
            EntityProperties(EntitySelector("domain2", "dev1", "suffix"), "mapping2"),
            EntityProperties(EntitySelector("domain1", "dev1", ANY_VALUE), "mapping3"),
            EntityProperties(EntitySelector(ANY_VALUE, "dev1", ANY_VALUE), "mapping4")
        )

        val resolver = EntityConfigurationResolver(properties)

        then(resolver.resolve(EntityId("domain1", "dev1", "suffix"))).matches { it?.mapping == "mapping1" }
        then(resolver.resolve(EntityId("domain2", "dev1", "suffix"))).matches { it?.mapping == "mapping2" }
        then(resolver.resolve(EntityId("domain1", "dev1", "another"))).matches { it?.mapping == "mapping3" }
        then(resolver.resolve(EntityId("unknown", "dev1", "another"))).matches { it?.mapping == "mapping4" }
    }

    @Test
    fun `should resolve configuration with multiple values in selector`() {
        val properties = EntitiesProperties(
            EntityProperties(EntitySelector(listOf("domain1"), "dev1", listOf("suffix1", "suffix2")), "mapping1"),
            EntityProperties(EntitySelector(listOf("domain2", "domain3"), "dev1", listOf("suffix3")), "mapping2")
        )

        val resolver = EntityConfigurationResolver(properties)

        then(resolver.resolve(EntityId("domain1", "dev1", "suffix1"))).matches { it?.mapping == "mapping1" }
        then(resolver.resolve(EntityId("domain1", "dev1", "suffix2"))).matches { it?.mapping == "mapping1" }
        then(resolver.resolve(EntityId("domain2", "dev1", "suffix3"))).matches { it?.mapping == "mapping2" }
        then(resolver.resolve(EntityId("domain3", "dev1", "suffix3"))).matches { it?.mapping == "mapping2" }
        then(resolver.resolve(EntityId("domain1", "dev1", "suffix3"))).isNull()
        then(resolver.resolve(EntityId("domain2", "dev1", "suffix4"))).isNull()
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
                    EntityProperties(EntitySelector(ANY_VALUE, "dev1", "suffix"), "other"),
                ),
                ElementalEntitySelector("domain1", "dev1", ANY_VALUE) to listOf(
                    EntityProperties(EntitySelector("domain1", "dev1", ANY_VALUE), "duplicated-domain-and-dev"),
                    EntityProperties(EntitySelector("domain1", "dev1", ANY_VALUE), "duplicated-domain-and-dev"),
                    EntityProperties(EntitySelector("domain1", "dev1", "suffix"), "other")
                ),
                ElementalEntitySelector("domain1", "dev1", "suffix") to listOf(
                    EntityProperties(EntitySelector("domain1", "dev1", "suffix"), "duplicated-everything"),
                    EntityProperties(EntitySelector("domain1", "dev1", "suffix"), "duplicated-everything"),
                    EntityProperties(EntitySelector("domain1", "dev1", "other"), "other")
                ),
                ElementalEntitySelector("domain1", "dev1", "suffix2") to listOf(
                    EntityProperties(
                        EntitySelector(listOf("domain1"), "dev1", listOf("suffix1", "suffix2")),
                        "overlapping-suffix"
                    ),
                    EntityProperties(
                        EntitySelector(listOf("domain1"), "dev1", listOf("suffix2", "suffix3")),
                        "overlapping-suffix"
                    )
                )
            ).stream()
    }
}

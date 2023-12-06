package dev.drzepka.smarthome.haexporter.infrastructure.provider

import dev.drzepka.smarthome.haexporter.domain.value.ValueType
import org.assertj.core.api.BDDAssertions.entry
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class YamlConfigurationPropertiesProviderTest {

    @Test
    fun `should load properties from file`() {
        System.setProperty("CONFIG_LOCATION", "classpath:application-config-test.yml")
        val root = YamlConfigurationPropertiesProvider.fromEnvironmentFile().root

        then(root.entities).hasSize(1)
        with(root.entities.first()) {
            then(selector.devices).containsExactly("test_device")
            then(selector.sensors).containsExactly("sensor1", "sensor2")
            then(schema).isEqualTo("energy")
        }

        then(root.schemas).hasSize(1)
        with(root.schemas.first()) {
            then(name).isEqualTo("schema_name")
            then(influxMeasurementName).isEqualTo("measurement_name")

            then(entities).hasSize(1)
            with(entities.first()) {
                then(sensor).isEqualTo("entity_sensor")
                then(type).isEqualTo(ValueType.INTEGER)
                then(stateMapping).isEqualTo("state_mapping")
                then(ignoredValues.matches("ignored_value")).isTrue
            }

            then(deviceNameMapping).containsExactly(entry("old_name", "new_name"))
        }

        then(root.stateMappings).hasSize(1)
        with(root.stateMappings.first()) {
            then(name).isEqualTo("binary_sensor")
            then(targetType).isEqualTo(ValueType.BOOLEAN)

            then(mappings).hasSize(1)
            with(mappings.first()) {
                then(from).isEqualTo("off")
                then(to).isEqualTo("false")
            }
        }
    }
}

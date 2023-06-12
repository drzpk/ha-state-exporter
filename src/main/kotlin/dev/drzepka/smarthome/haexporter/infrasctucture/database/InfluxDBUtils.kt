package dev.drzepka.smarthome.haexporter.infrasctucture.database

import com.influxdb.client.write.Point
import dev.drzepka.smarthome.haexporter.domain.value.BooleanStateValue
import dev.drzepka.smarthome.haexporter.domain.value.NumericStateValue
import dev.drzepka.smarthome.haexporter.domain.value.StateValue
import dev.drzepka.smarthome.haexporter.domain.value.StringStateValue

fun Point.addField(field: String, value: StateValue): Point {
    when (value) {
        is StringStateValue -> addField(field, value.stringValue)
        is NumericStateValue -> addField(field, value.numberValue)
        is BooleanStateValue -> addField(field, value.booleanValue)
    }

    return this
}

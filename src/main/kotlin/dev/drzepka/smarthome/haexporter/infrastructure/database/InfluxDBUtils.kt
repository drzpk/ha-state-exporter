package dev.drzepka.smarthome.haexporter.infrastructure.database

import com.influxdb.client.write.Point
import dev.drzepka.smarthome.haexporter.domain.value.BooleanStateValue
import dev.drzepka.smarthome.haexporter.domain.value.DoubleStateValue
import dev.drzepka.smarthome.haexporter.domain.value.LongStateValue
import dev.drzepka.smarthome.haexporter.domain.value.StateValue
import dev.drzepka.smarthome.haexporter.domain.value.StringStateValue

fun Point.addField(field: String, value: StateValue): Point {
    when (value) {
        is StringStateValue -> addField(field, value.stringValue)
        is LongStateValue -> addField(field, value.longValue)
        is BooleanStateValue -> addField(field, value.booleanValue)
        is DoubleStateValue -> addField(field, value.doubleValue)
    }

    return this
}

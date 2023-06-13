package dev.drzepka.smarthome.haexporter.infrastructure.database

import java.time.Instant
import kotlin.math.roundToLong

fun Instant.toDBDateTime(): String = toEpochMilli().let {
    "${it / 1000}.${it % 1000}"
}

fun Double.fromDBDateTime(): Instant = (this * 1000).roundToLong().let { Instant.ofEpochMilli(it) }

package dev.drzepka.smarthome.haexporter.domain.util

import java.time.Instant
import java.time.temporal.ChronoField

fun Instant.toEpochSecondDouble(): Double = toEpochMilli().toDouble() / 1000

fun Instant.trimToSeconds(): Instant = this.with(ChronoField.MICRO_OF_SECOND, 0)

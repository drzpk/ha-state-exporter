package dev.drzepka.smarthome.haexporter.util

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TestClock : Clock() {
    var localDate: LocalDate? = null
    var localTime: LocalTime? = null

    private val zoneId = ZoneId.systemDefault()

    override fun instant(): Instant {
        val date = localDate ?: LocalDate.now()
        val time = localTime ?: LocalTime.now()

        return date.atTime(time).atZone(zoneId).toInstant()
    }

    override fun withZone(zone: ZoneId?): Clock {
        throw NotImplementedError()
    }

    override fun getZone(): ZoneId = zoneId
}

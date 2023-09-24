package dev.drzepka.smarthome.haexporter.application.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.logging.log4j.kotlin.Logging
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

class JobScheduler(private val clock: Clock, private val scope: CoroutineScope) {

    fun scheduleJob(name: String, interval: Duration, job: (suspend () -> Unit)) {
        scope.launch {
            while (true) {
                var nextLaunch = getNextLaunchTime(interval)
                logger.debug { "Scheduling next launch of job '$name' at $nextLaunch (current time is ${clock.instant()})" }
                waitUntilNextLaunch(nextLaunch)

                nextLaunch = nextLaunch.plus(interval)
                runJobCatching(name, job)

                if (!clock.instant().isBefore(nextLaunch))
                    logger.warn { "Job '$name' has been running for too long and skipped next launch time" }

                // Cooldown for making this class easier to test
                delay(100)
            }
        }
    }

    private fun getNextLaunchTime(interval: Duration): Instant {
        val now = clock.instant()
        val nowMillis = now.toEpochMilli()
        val nextLaunchMillis = ceil(nowMillis.toDouble() / interval.toMillis()).toLong() * interval.toMillis()

        val nextLaunch = Instant.ofEpochMilli(nextLaunchMillis)
        if (!nextLaunch.isAfter(now))
            throw IllegalStateException("Expected next launch time ($nextLaunchMillis) to be after current time ($nowMillis)")

        return nextLaunch
    }

    private suspend fun waitUntilNextLaunch(nextLaunch: Instant) {
        val now = clock.instant()
        val delayMillis = nextLaunch.toEpochMilli() - now.toEpochMilli()
        logger.trace { "Waiting ${delayMillis}ms for the next launch" }
        delay(delayMillis)
    }

    private suspend fun runJobCatching(name: String, job: (suspend () -> Unit)) {
        try {
            job()
        } catch (e: Exception) {
            logger.error(e) { "Error while executing job '$name'" }
        }
    }

    companion object : Logging
}

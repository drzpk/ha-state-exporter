package dev.drzepka.smarthome.haexporter.application.service

import dev.drzepka.smarthome.haexporter.util.TestClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

@Timeout(3, unit = TimeUnit.SECONDS)
@OptIn(ExperimentalCoroutinesApi::class)
internal class JobSchedulerTest {

    private val clock = TestClock()

    @Test
    fun `should schedule a job in given interval`() = runTest {
        val counter = AtomicInteger(0)
        clock.localTime = LocalTime.now()

        getScheduler().scheduleJob("job-1", Duration.ofSeconds(3)) {
            counter.incrementAndGet()
        }

        waitForValue(counter, 1, 3000)
        then(counter.get()).isEqualTo(1)

        advanceTime(2000)
        then(counter.get()).isEqualTo(1)

        advanceTime(900)
        then(counter.get()).isEqualTo(1)

        advanceTime(110)
        then(counter.get()).isEqualTo(2)
    }

    @ParameterizedTest
    @MethodSource("alignmentTestData")
    fun `should align job time to clock`(data: AlignmentTestData) = runTest {
        val allowedInaccuracy = 100
        val counter = AtomicInteger(0)
        clock.localTime = data.parseStartTime()

        getScheduler().scheduleJob("job-1", data.duration) {
            counter.incrementAndGet()
        }

        advanceTime(data.timeUntilLaunch() - allowedInaccuracy)
        then(counter.get()).isEqualTo(0)

        advanceTime(allowedInaccuracy * 2)
        then(counter.get()).isEqualTo(1)
    }

    @Test
    fun `should stop executing job after scope cancellation`(): Unit = runBlocking {
        val counter = AtomicInteger(0)

        val scope = CoroutineScope(Dispatchers.Unconfined + Job())
        val scheduler = JobScheduler(clock, scope)

        scheduler.scheduleJob("job-1", Duration.ofMillis(400)) {
            counter.incrementAndGet()
        }

        delay(600)
        val value = counter.get()
        then(value).isGreaterThan(0)

        scope.cancel()
        delay(600)
        then(counter.get()).isEqualTo(value)
    }

    @Test
    fun `job failure shouldn't affect future launches`() = runTest {
        val counter = AtomicInteger(0)
        clock.localTime = LocalTime.now()

        getScheduler().scheduleJob("job-1", Duration.ofSeconds(1)) {
            val value = counter.getAndIncrement()
            if (value == 0)
                throw Exception("Something bad happened")
        }

        waitForValue(counter, 1, 1000)
        then(counter.get()).isEqualTo(1)

        advanceTime(1000)
        then(counter.get()).isEqualTo(2)
    }

    @Test
    fun `job failure shouldn't affect other jobs`() = runTest {
        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)
        clock.localTime = LocalTime.now()

        getScheduler().scheduleJob("job-1", Duration.ofSeconds(1)) {
            val value = counter1.getAndIncrement()
            if (value == 0)
                throw Exception("Something bad happened")
        }
        getScheduler().scheduleJob("job-2", Duration.ofSeconds(1)) {
           counter2.incrementAndGet()
        }

        waitForValue(counter1, 1, 1000)
        then(counter1.get()).isEqualTo(1)
        then(counter2.get()).isEqualTo(1)

        advanceTime(1000)
        then(counter1.get()).isEqualTo(2)
        then(counter2.get()).isEqualTo(2)
    }

    private suspend fun TestScope.waitForValue(counter: AtomicInteger, expectedValue: Int, timeout: Long) {
        withTimeout(timeout) {
            while (true) {
                if (counter.get() >= expectedValue)
                    break
                advanceTime(1)
            }
        }
    }

    private fun TestScope.advanceTime(by: Int) {
        repeat(by) {
            clock.localTime = clock.localTime!!.plus(Duration.ofMillis(1))
            testScheduler.advanceTimeBy(1)
        }
    }

    private fun TestScope.getScheduler() = JobScheduler(clock, backgroundScope)

    companion object {
        @JvmStatic
        private fun alignmentTestData(): Stream<AlignmentTestData> = Stream.of(
            AlignmentTestData("14:08:21", Duration.ofSeconds(5), "14:08:25"),
            AlignmentTestData("14:08:25", Duration.ofSeconds(5), "14:08:30"),
            AlignmentTestData("17:12:25", Duration.ofMinutes(1), "17:13:00"),
            AlignmentTestData("17:12:00", Duration.ofMinutes(15), "17:15:00"),
        )

        data class AlignmentTestData(val startTime: String, val duration: Duration, val nextLaunchTime: String) {
            fun timeUntilLaunch(): Int = ((parseNextLaunchTime().toNanoOfDay() - parseStartTime().toNanoOfDay()) / 1_000_000).toInt()
            fun parseStartTime(): LocalTime = LocalTime.parse(startTime)
            private fun parseNextLaunchTime(): LocalTime = LocalTime.parse(nextLaunchTime)
        }
    }
}

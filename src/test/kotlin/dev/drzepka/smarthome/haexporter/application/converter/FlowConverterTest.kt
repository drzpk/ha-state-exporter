package dev.drzepka.smarthome.haexporter.application.converter

import dev.drzepka.smarthome.haexporter.application.model.TimeData
import dev.drzepka.smarthome.haexporter.domain.util.trimToSeconds
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant

@Timeout(3)
internal class FlowConverterTest {

    @Test
    fun `should return flow using a single batch`() = runBlocking {
        val time = Instant.now().trimToSeconds()
        val source = TestCollectionSource(
            TestData(1, time.plusSeconds(1)),
            TestData(2, time.plusSeconds(2)),
            TestData(3, time.plusSeconds(3)),
            TestData(4, time.plusSeconds(4)),
            TestData(5, time.plusSeconds(5))
        )

        val converter = FlowConverter(4, source::getCollection)
        val result = converter.execute(time.plusSeconds(2), 4).toList(mutableListOf())

        then(source.calls).isEqualTo(1)
        then(result).hasSize(4)
        then(result).extracting("id").containsExactly(2, 3, 4, 5)

        Unit
    }

    @Test
    fun `should return flow using multiple batches with the last one being full`() = runBlocking {
        val time = Instant.now().trimToSeconds()
        val source = TestCollectionSource(
            TestData(1, time.plusSeconds(1)),
            TestData(2, time.plusSeconds(2)),
            TestData(3, time.plusSeconds(3)),
            TestData(4, time.plusSeconds(4)),
            TestData(5, time.plusSeconds(5)),
            TestData(6, time.plusSeconds(6))
        )

        val converter = FlowConverter(3, source::getCollection)
        val result = converter.execute(time, 10).toList(mutableListOf())

        then(source.calls).isEqualTo(3)
        then(result).hasSize(6)
        then(result).extracting("id").containsExactly(1, 2, 3, 4, 5, 6)

        Unit
    }

    @Test
    fun `should return flow using multiple batches with the last one being partial`() = runBlocking {
        val time = Instant.now().trimToSeconds()
        val source = TestCollectionSource(
            TestData(1, time.plusSeconds(1)),
            TestData(2, time.plusSeconds(2)),
            TestData(3, time.plusSeconds(3)),
            TestData(4, time.plusSeconds(4)),
            TestData(5, time.plusSeconds(5)),
            TestData(6, time.plusSeconds(6))
        )

        val converter = FlowConverter(4, source::getCollection)
        val result = converter.execute(time, 10).toList(mutableListOf())

        then(source.calls).isEqualTo(2)
        then(result).hasSize(6)
        then(result).extracting("id").containsExactly(1, 2, 3, 4, 5, 6)

        Unit
    }

    @Test
    fun `should return empty flow if source collection is empty`() = runBlocking {
        val source = TestCollectionSource<TestData>()

        val converter = FlowConverter(4, source::getCollection)
        val result = converter.execute(Instant.now(), 10).toList(mutableListOf())

        then(source.calls).isEqualTo(1)
        then(result).hasSize(0)

        Unit
    }

    private class TestCollectionSource<T : TimeData>(vararg data: T) {
        private val source: Collection<T> = data.toList()

        var calls = 0

        suspend fun getCollection(from: Instant, offset: Int, limit: Int): Collection<T> {
            calls++

            return source
                .asFlow()
                .dropWhile { it.time < from }
                .drop(offset)
                .take(limit)
                .toList(mutableListOf())
        }
    }

    private data class TestData(val id: Int, override val time: Instant) : TimeData
}

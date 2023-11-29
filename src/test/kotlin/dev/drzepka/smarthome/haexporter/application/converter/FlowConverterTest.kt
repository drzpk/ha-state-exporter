package dev.drzepka.smarthome.haexporter.application.converter

import dev.drzepka.smarthome.haexporter.application.model.SourceStateQuery
import dev.drzepka.smarthome.haexporter.domain.util.trimToSeconds
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import dev.drzepka.smarthome.haexporter.domain.value.strategy.WorkUnit
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant

@Timeout(3)
internal class FlowConverterTest {

    @Nested
    inner class SingleWorkUnit {
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

            val converter = FlowConverter(4, 4, source::getCollection)
            val result = converter.execute(time.plusSeconds(2).singleWorkUnit()).toList(mutableListOf())

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

            val converter = FlowConverter(3, 10, source::getCollection)
            val result = converter.execute(time.singleWorkUnit()).toList(mutableListOf())

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

            val converter = FlowConverter(4, 10, source::getCollection)
            val result = converter.execute(time.singleWorkUnit()).toList(mutableListOf())

            then(source.calls).isEqualTo(2)
            then(result).hasSize(6)
            then(result).extracting("id").containsExactly(1, 2, 3, 4, 5, 6)

            Unit
        }

        @Test
        fun `should return empty flow if source collection is empty`() = runBlocking {
            val source = TestCollectionSource()

            val converter = FlowConverter(4, 10, source::getCollection)
            val result = converter.execute(Instant.now().singleWorkUnit()).toList(mutableListOf())

            then(source.calls).isEqualTo(1)
            then(result).hasSize(0)

            Unit
        }

        private fun Instant.singleWorkUnit() = listOf(WorkUnit(this, null))
    }

    @Nested
    inner class MultipleWorkUnits {
        @Test
        fun `should correctly process multiple work units`() = runBlocking{
            val time = Instant.now().trimToSeconds()
            val source = TestCollectionSource(
                TestData(1, time.plusSeconds(1)),
                TestData(1, time.plusSeconds(2)),
                TestData(1, time.plusSeconds(3)),

                TestData(2, time.plusSeconds(2)),
                TestData(3, time.plusSeconds(3)),
                TestData(2, time.plusSeconds(4)),
                TestData(3, time.plusSeconds(5)),
                TestData(2, time.plusSeconds(6)),
                TestData(3, time.plusSeconds(7)),
            )
            val workUnits = listOf(
                WorkUnit(time.plusSeconds(2), setOf(entityId(1))),
                WorkUnit(time.plusSeconds(2), setOf(entityId(2), entityId(3))),
            )

            val converter = FlowConverter(3, 20, source::getCollection)
            val result = converter.execute(workUnits).toList()

            then(source.queries).containsExactly(
                SourceStateQuery(time.plusSeconds(2), setOf(entityIdString(1)), 0, 3),
                SourceStateQuery(time.plusSeconds(2), setOf(entityIdString(2), entityIdString(3)), 0, 3),
                SourceStateQuery(time.plusSeconds(2), setOf(entityIdString(2), entityIdString(3)), 3, 3),
                SourceStateQuery(time.plusSeconds(2), setOf(entityIdString(2), entityIdString(3)), 6, 3),
            )
            then(result).containsExactly(*source.elements.subList(1, source.elements.size).toTypedArray())

            Unit
        }

        @Test
        fun `should respect item limit`() = runBlocking{
            val time = Instant.now().trimToSeconds()
            val source = TestCollectionSource(
                TestData(1, time.plusSeconds(1)),
                TestData(1, time.plusSeconds(2)),
                TestData(1, time.plusSeconds(3)),

                TestData(2, time.plusSeconds(2)),
                TestData(3, time.plusSeconds(3)),
                TestData(2, time.plusSeconds(4)),
            )
            val workUnits = listOf(
                WorkUnit(time.plusSeconds(2), setOf(entityId(1))),
                WorkUnit(time.plusSeconds(2), setOf(entityId(2), entityId(3))),
            )

            val converter = FlowConverter(3, 4, source::getCollection)
            val result = converter.execute(workUnits).toList()

            then(source.queries).containsExactly(
                SourceStateQuery(time.plusSeconds(2), setOf(entityIdString(1)), 0, 3),
                SourceStateQuery(time.plusSeconds(2), setOf(entityIdString(2), entityIdString(3)), 0, 2),
            )
            then(result).containsExactly(
                source.elements[1],
                source.elements[2],
                source.elements[3],
                source.elements[4]
            )

            Unit
        }
    }

    private class TestCollectionSource(vararg data: TestData) {
        val elements: List<TestData> = data.toList()
        val queries = mutableListOf<SourceStateQuery>()
        val calls: Int
            get() = queries.size

        suspend fun getCollection(query: SourceStateQuery): Collection<TestData> {
            queries.add(query)
            return elements
                .asFlow()
                .filter { it.time >= query.from }
                .filter { query.entities == null || entityIdString(it.id) in query.entities!! }
                .drop(query.offset)
                .take(query.limit)
                .toList(mutableListOf())
        }
    }

    private data class TestData(val id: Int, val time: Instant)

    companion object {
        private fun entityIdString(id: Int) = entityId(id).toString()
        private fun entityId(id: Int) = EntityId("class", "dev", "sensor-$id")
    }
}

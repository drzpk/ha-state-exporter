package dev.drzepka.smarthome.haexporter.application.converter

import dev.drzepka.smarthome.haexporter.application.model.SourceStateQuery
import dev.drzepka.smarthome.haexporter.domain.value.strategy.WorkUnit
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.apache.logging.log4j.kotlin.Logging

class FlowConverter<T>(
    private val batchSize: Int,
    private val limit: Int,
    private val collectionSource: suspend (SourceStateQuery) -> Collection<T>
) {

    fun execute(workUnits: List<WorkUnit>): Flow<T> = channelFlow {
        logger.debug { "Starting execution with ${workUnits.size} work unit(s) (batchSize=$batchSize, limit=$limit)" }

        Executor(workUnits.listIterator(), this).execute()
        close()

        logger.debug { "Finished execution" }
    }

    private inner class Executor(
        private val workUnitIterator: Iterator<WorkUnit>,
        private val target: ProducerScope<T>
    ) {

        private var totalProcessed = 0

        suspend fun execute() {
            var workUnitNo = 0
            while (workUnitIterator.hasNext() && totalProcessed < limit) {
                val workUnit = workUnitIterator.next()
                logger.debug { "Processing work unit #$workUnitNo: $workUnit, totalProcessed=$totalProcessed" }
                processWorkUnit(workUnit)
                workUnitNo++
            }
        }

        private suspend fun processWorkUnit(unit: WorkUnit) {
            var offset = 0
            var batchNo = 0

            do {
                logger.debug { "Processing batch #$batchNo" }
                val nextBatchSize = minOf(limit - totalProcessed, batchSize)
                val query = SourceStateQuery(
                    unit.from,
                    unit.entityFilter?.map { it.toString() }?.toSet(),
                    offset,
                    nextBatchSize
                )

                val batch = collectionSource.invoke(query)
                logger.debug { "Batch #$batchNo: received collection with size=${batch.size}, offset=$offset, totalProcessed=$totalProcessed" }

                batch.forEach { target.send(it) }
                offset += batch.size
                totalProcessed += batch.size
                batchNo++
            } while (totalProcessed < limit && batch.size == nextBatchSize)
        }
    }

    companion object : Logging
}

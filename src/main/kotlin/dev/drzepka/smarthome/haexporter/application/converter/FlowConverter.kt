package dev.drzepka.smarthome.haexporter.application.converter

import dev.drzepka.smarthome.haexporter.application.model.TimeData
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.apache.logging.log4j.kotlin.Logging
import java.time.Instant

class FlowConverter<T : TimeData>(
    private val batchSize: Int,
    private val collectionSource: suspend (from: Instant, offset: Int, limit: Int) -> Collection<T>
) {

    fun execute(from: Instant, limit: Int): Flow<T> = channelFlow {
        logger.debug { "Starting execution with batchSize=$batchSize, from=$from, limit=$limit" }

        Executor(from, limit, this).execute()
        close()

        logger.debug { "Finished execution" }
    }

    private inner class Executor(
        private val startTime: Instant,
        private val limit: Int,
        private val target: ProducerScope<T>
    ) {

        var batchNo = 0
        var totalProcessed = 0

        suspend fun execute() {
            do {
                logger.debug { "Processing batch #$batchNo" }

                val nextBatchSize = minOf(limit - totalProcessed, batchSize)
                val batch = collectionSource.invoke(startTime, totalProcessed, nextBatchSize)
                totalProcessed += batch.size

                logger.debug { "Batch #$batchNo: received collection with size=${batch.size}, totalProcessed=$totalProcessed" }

                batch.forEach {
                    target.send(it)
                }

                batchNo++
            } while (totalProcessed < limit && batch.size == nextBatchSize)
        }
    }

    companion object : Logging
}

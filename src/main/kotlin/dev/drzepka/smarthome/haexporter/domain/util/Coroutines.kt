package dev.drzepka.smarthome.haexporter.domain.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> {
    val chunk = mutableListOf<T>()

    return flow {
        this@chunked.collect {
            chunk.add(it)

            if (chunk.size == size) {
                emit(chunk.toList())
                chunk.clear()
            }
        }

        if (chunk.isNotEmpty())
            emit(chunk.toList())
    }
}

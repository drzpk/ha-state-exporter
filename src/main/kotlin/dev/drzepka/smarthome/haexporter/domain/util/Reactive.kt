package dev.drzepka.smarthome.haexporter.domain.util

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.reactivestreams.Publisher

fun <T> Publisher<T>.blockingGet(): T = runBlocking {
    awaitFirst()
}

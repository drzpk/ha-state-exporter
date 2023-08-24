package dev.drzepka.smarthome.haexporter

import org.koin.dsl.koinApplication
import org.koin.fileProperties

fun main() {
    koinApplication {
        fileProperties()
    }
}

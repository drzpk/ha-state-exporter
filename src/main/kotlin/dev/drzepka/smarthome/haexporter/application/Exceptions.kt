package dev.drzepka.smarthome.haexporter.application

import dev.drzepka.smarthome.haexporter.domain.value.ElementalEntitySelector

class DuplicatedEntitySelectorsException(val duplicates: List<ElementalEntitySelector>) :
        RuntimeException("Found duplicated entity selectors: \n${duplicates.prettyPrint()}") {

    companion object {
        private fun List<ElementalEntitySelector>.prettyPrint(): String = joinToString(separator = "\n") { " - $it" }
    }
}

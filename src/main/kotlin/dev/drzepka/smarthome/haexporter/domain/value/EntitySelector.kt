package dev.drzepka.smarthome.haexporter.domain.value

data class EntitySelector(
    val domains: List<String> = listOf(ANY_VALUE),
    val device: String,
    val suffixes: List<String>? = listOf(ANY_VALUE)
) {
    constructor(domain: String, device: String, suffix: String?)
        : this(listOf(domain), device, if (suffix != null) listOf(suffix) else null)

    fun toElementalSelectors(): List<ElementalEntitySelector> = domains.flatMap { domain ->
        if (!suffixes.isNullOrEmpty())
            suffixes.map { suffix -> ElementalEntitySelector(domain, device, suffix) }
        else
            listOf(ElementalEntitySelector(domain, device, null))
    }

    fun matches(entityId: EntityId): Boolean = entityId.let {
        matchesDomain(it.domainValue) && matchesDevice(it.device) && matchesSuffix(it.suffix)
    }

    private fun matchesDomain(domain: String): Boolean = domains.any { it == domain || it == ANY_VALUE }

    private fun matchesDevice(device: String): Boolean = this.device == device

    private fun matchesSuffix(suffix: String?): Boolean = suffixes == null && suffix == null
        || suffixes?.any { it == suffix || it == ANY_VALUE } ?: false
}

data class ElementalEntitySelector(val domain: String, val device: String, val suffix: String?) {
    override fun toString(): String = "domain: $domain, device: $device, suffix: $suffix"
}

const val ANY_VALUE = "*"

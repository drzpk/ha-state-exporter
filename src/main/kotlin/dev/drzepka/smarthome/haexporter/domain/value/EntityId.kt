package dev.drzepka.smarthome.haexporter.domain.value

data class EntityId(
    val domainValue: String,
    val device: String,
    val suffix: String?
) {

    val domain: Domain?
        get() = Domain.fromString(domainValue)

    override fun toString(): String {
        var str = "$domainValue.${device}"
        if (suffix != null)
            str += "_$suffix"
        return str
    }
}

package dev.drzepka.smarthome.haexporter.domain.util

class MultiValueMatcher(patterns: List<String>) {
    private val matchers = patterns.map { MatcherFactory.createMatcher(it) }

    constructor(pattern: String) : this(listOf(pattern))

    fun matches(input: String): Boolean {
        for (matcher in matchers) {
            when (matcher.match(input)) {
                MatchResult.MATCH_FOUND -> return true
                MatchResult.INTERRUPT -> return false
                else -> {}
            }
        }

        return false
    }
}

private object MatcherFactory {
    private const val NEGATION_SYMBOL = '!'
    private const val WILDCARD_SYMBOL = "*"
    private const val REGEX_SYMBOL = '/'

    private val REGEX_OPTION_MAPPING = mapOf(
        'i' to RegexOption.IGNORE_CASE,
        'm' to RegexOption.MULTILINE,
        'l' to RegexOption.LITERAL,
        'd' to RegexOption.DOT_MATCHES_ALL
    )

    fun createMatcher(pattern: String): Matcher {
        try {
            return if (pattern.startsWith(NEGATION_SYMBOL))
                NegationMatcher(createSimpleMatcher(pattern.substring(1)))
            else createSimpleMatcher(pattern)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error while creating matcher for pattern: $pattern", e)
        }
    }

    private fun createSimpleMatcher(pattern: String): Matcher {
        if (pattern == WILDCARD_SYMBOL)
            return WildcardMatcher

        return createRegexMatcher(pattern) ?: LiteralMatcher(pattern)
    }

    private fun createRegexMatcher(pattern: String): RegexMatcher? {
        if (!pattern.startsWith(REGEX_SYMBOL))
            return null

        val endRegexSymbolPos = pattern.lastIndexOf(REGEX_SYMBOL)
        if (endRegexSymbolPos == -1 || endRegexSymbolPos == 0)
            return null

        val flags = pattern.substringAfterLast(REGEX_SYMBOL, "").associateWith { REGEX_OPTION_MAPPING[it] }
        if (flags.count { it.value == null } > 0) {
            val unrecognizedFlags = flags.filter { it.value == null }.keys.joinToString()
            throw IllegalArgumentException("Unrecognized regex flags: $unrecognizedFlags")
        }

        val regex = Regex(pattern.substring(1, endRegexSymbolPos), flags.values.filterNotNull().toSet())
        return RegexMatcher(regex)
    }
}

private enum class MatchResult {
    MATCH_FOUND, CONTINUE, INTERRUPT
}

private interface Matcher {
    fun match(value: String): MatchResult
}

private class LiteralMatcher(private val value: String) : Matcher {
    override fun match(value: String): MatchResult =
        if (this.value == value) MatchResult.MATCH_FOUND else MatchResult.CONTINUE
}

private class RegexMatcher(private val regex: Regex) : Matcher {
    override fun match(value: String): MatchResult =
        if (regex.matches(value)) MatchResult.MATCH_FOUND else MatchResult.CONTINUE
}

private object WildcardMatcher : Matcher {
    override fun match(value: String): MatchResult = MatchResult.MATCH_FOUND
}

private class NegationMatcher(private val other: Matcher) : Matcher {
    override fun match(value: String): MatchResult =
        if (other.match(value) in listOf(MatchResult.MATCH_FOUND, MatchResult.INTERRUPT))
            MatchResult.INTERRUPT
        else MatchResult.CONTINUE
}

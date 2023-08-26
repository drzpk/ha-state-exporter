package dev.drzepka.smarthome.haexporter.domain.util

import org.assertj.core.api.BDDAssertions.assertThatIllegalArgumentException
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

internal class MultiValueMatcherTest {

    @Test
    fun `should match single literal value`() {
        val matcher = MultiValueMatcher("single_value")

        then(matcher.matches("single_value")).isTrue
        then(matcher.matches("single_value ")).isFalse
        then(matcher.matches("something else")).isFalse
    }

    @Test
    fun `should match single regex value`() {
        val matcher = MultiValueMatcher("""/^http(s)?://(?:www\.)?test\.com$/""")

        then(matcher.matches("https://www.test.com")).isTrue
        then(matcher.matches("https://test.com")).isTrue
        then(matcher.matches("https://test.com ")).isFalse
        then(matcher.matches(" https://test.com")).isFalse
        then(matcher.matches("test")).isFalse
        then(matcher.matches("https://test")).isFalse
    }

    @Test
    fun `should match single regex value with flags`() {
        val matcher = MultiValueMatcher("""/ignore[\s-_]case/i""")

        then(matcher.matches("ignore_case")).isTrue
        then(matcher.matches("IGNORE-CASE")).isTrue
        then(matcher.matches("Ignore Case")).isTrue
        then(matcher.matches("something else")).isFalse
        then(matcher.matches("abc")).isFalse
    }

    @Test
    fun `should treat malformed regex expression as literal value`() {
        val matcher = MultiValueMatcher("""/te(?:sSxX)t""")

        then(matcher.matches("test")).isFalse
        then(matcher.matches("teXt")).isFalse
        then(matcher.matches("/te(?:sSxX)t")).isTrue
    }

    @Test
    fun `should throw exception if regex pattern is used with unknown flags`() {
        assertThatIllegalArgumentException()
            .isThrownBy { MultiValueMatcher("""/test/x""") }
            .havingRootCause()
            .withMessage("Unrecognized regex flags: x")
    }

    @Test
    fun `should match wildcard value`() {
        val matcher = MultiValueMatcher("*")

        then(matcher.matches("any text")).isTrue
        then(matcher.matches("*")).isTrue
    }

    @Test
    fun `should match negation`() {
        val matcher = MultiValueMatcher(listOf("!off", "*"))

        then(matcher.matches("any text")).isTrue
        then(matcher.matches("off")).isFalse
    }

    @Test
    fun `should match multiple value using multiple patterns`() {
        val matcher = MultiValueMatcher(listOf(
            "temp_outside",
            "temp_inside",
            "/temp_room_\\d+/"
        ))

        then(matcher.matches("temp_outside")).isTrue
        then(matcher.matches("temp_inside")).isTrue
        then(matcher.matches("temp_room_2")).isTrue
        then(matcher.matches("temp_room_x")).isFalse
        then(matcher.matches("unknown")).isFalse
    }

    @Test
    fun `should match patterns from top to bottom`() {
        val matcher = MultiValueMatcher(listOf(
            "value-1",
            "value-2",
            "!value-1",
            "!valid-2",
            "/valid-.*/"
        ))

        then(matcher.matches("value-1")).isTrue
        then(matcher.matches("value-2")).isTrue
        then(matcher.matches("valid-1")).isTrue
        then(matcher.matches("valid-2")).isFalse
        then(matcher.matches("valid-3")).isTrue
    }
}

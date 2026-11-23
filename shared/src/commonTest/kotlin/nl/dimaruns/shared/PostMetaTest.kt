package nl.dimaruns.shared

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostMetaTest {
    private val valid = PostMeta(title = "Hello", date = LocalDate(2026, 11, 23), tags = listOf("kotlin", "edge"))

    @Test
    fun validPostHasNoProblems() {
        assertTrue(valid.validate().isEmpty())
        assertEquals("hello", valid.resolvedSlug.value)
    }

    @Test
    fun explicitSlugWins() {
        assertEquals("custom", valid.copy(slug = "custom").resolvedSlug.value)
    }

    @Test
    fun reportsAllProblemsAtOnce() {
        val problems = valid.copy(title = " ", slug = "Bad Slug", tags = listOf("Kotlin", "x", "x")).validate()
        assertEquals(4, problems.size, problems.toString())
    }
}

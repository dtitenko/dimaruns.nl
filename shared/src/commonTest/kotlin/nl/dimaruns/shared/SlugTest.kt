package nl.dimaruns.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SlugTest {
    @Test
    fun derivesSlugFromTitle() {
        assertEquals("hello-edge-world", Slug.fromTitle("Hello, Edge World!").value)
        assertEquals("kotlin-2-4-at-the-edge", Slug.fromTitle("  Kotlin 2.4 at the edge  ").value)
    }

    @Test
    fun rejectsInvalidSlugs() {
        listOf("", "Upper", "double--dash", "-lead", "trail-", "spa ce", "a".repeat(81)).forEach {
            assertNull(Slug.parse(it), "expected '$it' to be rejected")
        }
    }

    @Test
    fun acceptsValidSlug() {
        assertEquals("my-post-2", Slug.parse("my-post-2")?.value)
    }

    @Test
    fun failsOnTitleWithoutSlugCharacters() {
        assertFailsWith<IllegalArgumentException> { Slug.fromTitle("!!!") }
    }
}

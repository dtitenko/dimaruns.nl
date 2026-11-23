package dimaruns.shared

import kotlin.jvm.JvmInline

/**
 * URL-safe identifier of a post: lowercase ASCII letters, digits and single hyphens.
 * Shared by the generator (to build paths) and the edge Worker (to validate requests).
 */
@JvmInline
value class Slug private constructor(
    val value: String,
) {
    override fun toString(): String = value

    companion object {
        private val VALID = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
        private val NON_SLUG_CHARS = Regex("[^a-z0-9]+")
        const val MAX_LENGTH = 80

        fun parse(raw: String): Slug? = raw.takeIf { it.length <= MAX_LENGTH && VALID.matches(it) }?.let(::Slug)

        /** Derives a slug from a free-form title, e.g. "Hello, Edge World!" -> "hello-edge-world". */
        fun fromTitle(title: String): Slug {
            val slug =
                title
                    .lowercase()
                    .replace(NON_SLUG_CHARS, "-")
                    .trim('-')
                    .take(MAX_LENGTH)
                    .trimEnd('-')
            require(slug.isNotEmpty()) { "Cannot derive a slug from title '$title'" }
            return Slug(slug)
        }
    }
}

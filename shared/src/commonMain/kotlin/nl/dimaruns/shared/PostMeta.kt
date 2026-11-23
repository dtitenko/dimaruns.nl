package nl.dimaruns.shared

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

const val MAX_SUMMARY_LENGTH = 280

/** Front-matter of a blog post, as written at the top of each Markdown file in `content/posts`. */
@Serializable
data class PostMeta(
    val title: String,
    val date: LocalDate,
    val slug: String? = null,
    val summary: String? = null,
    val tags: List<String> = emptyList(),
    val draft: Boolean = false,
) {
    /** Explicit slug if given, otherwise derived from the title. */
    val resolvedSlug: Slug
        get() =
            slug?.let { Slug.parse(it) ?: error("Invalid slug '$it' in post '$title'") }
                ?: Slug.fromTitle(title)
}

/** Validation problems are collected rather than thrown, so a build can report all of them at once. */
fun PostMeta.validate(): List<String> =
    buildList {
        if (title.isBlank()) add("title must not be blank")
        if (slug != null && Slug.parse(slug) == null) add("slug '$slug' is not a valid slug")
        if (summary != null && summary.length > MAX_SUMMARY_LENGTH) {
            add("summary exceeds $MAX_SUMMARY_LENGTH characters")
        }
        tags.filter { Slug.parse(it) == null }.forEach { add("tag '$it' must be lowercase-kebab-case") }
        if (tags.size != tags.toSet().size) add("tags contain duplicates")
    }

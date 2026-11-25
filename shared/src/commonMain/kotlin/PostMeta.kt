package dimaruns.shared

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
    val image: String? = null,
    val imageAlt: String = "",
    val numbered: Boolean = false,
    val results: List<RaceResult> = emptyList(),
) {
    /** Explicit slug if given, otherwise derived from the title. */
    val resolvedSlug: Slug
        get() =
            slug?.let { Slug.parse(it) ?: error("Invalid slug '$it' in post '$title'") }
                ?: Slug.fromTitle(title)
}

@Serializable
data class RaceResult(
    val race: String,
    val date: String,
    val time: String,
    val note: String = "",
    val win: Boolean = false,
)

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
        if (image != null && !image.startsWith("/")) add("image '$image' must be a root-relative path")
    }

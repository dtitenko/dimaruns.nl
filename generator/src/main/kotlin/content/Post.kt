package dimaruns.generator.content

import dimaruns.shared.PostMeta
import dimaruns.shared.Slug

/** A post ready to be rendered: validated metadata plus its rendered HTML body. */
data class Post(
    val source: String,
    val meta: PostMeta,
    val slug: Slug,
    val html: String,
    val excerpt: String,
    val readingMinutes: Int,
) {
    /** Root-relative URL of the post page. Root-relative links keep preview deploys working on any host. */
    val path: String get() = "/posts/$slug/"
}

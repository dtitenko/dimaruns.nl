package nl.dimaruns.generator.content

import com.charleskorn.kaml.Yaml
import nl.dimaruns.generator.render.MarkdownRenderer
import nl.dimaruns.shared.PostMeta
import nl.dimaruns.shared.validate
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.math.ceil

private const val WORDS_PER_MINUTE = 200.0
private const val MAX_EXCERPT_LENGTH = 200

/** Loads and validates all posts. Fails with every problem found, not just the first one. */
class PostLoader(
    private val markdown: MarkdownRenderer = MarkdownRenderer(),
    private val yaml: Yaml = Yaml.default,
) {
    fun load(
        postsDir: Path,
        includeDrafts: Boolean,
    ): List<Post> {
        val problems = mutableListOf<Problem>()
        val posts =
            findPosts(postsDir).mapNotNull { path ->
                val source = postsDir.relativize(path).toString().replace('\\', '/')
                try {
                    parse(source, path.readText()).also { problems += it.problems }.post
                } catch (e: IllegalArgumentException) {
                    // kaml and kotlinx.serialization errors are IllegalArgumentException subclasses as well.
                    problems += Problem(source, e.message ?: e.toString())
                    null
                }
            }

        posts
            .groupBy { it.slug }
            .filterValues { it.size > 1 }
            .forEach { (slug, duplicates) ->
                problems += Problem(duplicates.joinToString { it.source }, "duplicate slug '$slug'")
            }

        if (problems.isNotEmpty()) throw ContentException(problems)

        return posts
            .filter { includeDrafts || !it.meta.draft }
            .sortedWith(compareByDescending<Post> { it.meta.date }.thenBy { it.slug.value })
    }

    private class ParseResult(
        val post: Post?,
        val problems: List<Problem>,
    )

    private fun parse(
        source: String,
        text: String,
    ): ParseResult {
        val document = splitFrontMatter(text)
        val meta = yaml.decodeFromString(PostMeta.serializer(), document.frontMatter)
        val metaProblems = meta.validate().map { Problem(source, it) }
        if (metaProblems.isNotEmpty()) return ParseResult(null, metaProblems)

        val rendered = markdown.render(document.body)
        val post =
            Post(
                source = source,
                meta = meta,
                slug = meta.resolvedSlug,
                html = rendered.html,
                excerpt = meta.summary ?: rendered.firstParagraph.truncate(MAX_EXCERPT_LENGTH),
                readingMinutes = ceil(rendered.wordCount / WORDS_PER_MINUTE).toInt().coerceAtLeast(1),
            )
        return ParseResult(post, emptyList())
    }
}

private fun String.truncate(max: Int): String =
    if (length <= max) this else take(max).substringBeforeLast(' ').trimEnd(',', '.', ';', ':') + "…"

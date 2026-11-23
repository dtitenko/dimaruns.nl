package dimaruns.generator.content

/** A Markdown source file split into its YAML front-matter and Markdown body. */
data class SourceDocument(
    val frontMatter: String,
    val body: String,
)

private val DELIMITER = Regex("^---\\s*$")

/**
 * Splits a document of the form:
 * ```
 * ---
 * title: Hello
 * ---
 * Markdown body
 * ```
 */
fun splitFrontMatter(text: String): SourceDocument {
    val lines = text.removePrefix("﻿").lines()
    require(lines.isNotEmpty() && DELIMITER.matches(lines.first())) {
        "missing front-matter: the file must start with a '---' line"
    }
    val closing = lines.drop(1).indexOfFirst { DELIMITER.matches(it) } + 1
    require(closing > 0) { "unterminated front-matter: no closing '---' line" }
    return SourceDocument(
        frontMatter = lines.subList(1, closing).joinToString("\n"),
        body = lines.drop(closing + 1).joinToString("\n"),
    )
}

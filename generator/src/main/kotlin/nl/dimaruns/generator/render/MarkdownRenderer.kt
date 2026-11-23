package nl.dimaruns.generator.render

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.text.TextContentRenderer

data class RenderedMarkdown(
    val html: String,
    val wordCount: Int,
    val firstParagraph: String,
)

private val WHITESPACE = Regex("\\s+")

/**
 * CommonMark + GFM tables, strikethrough and autolinks. Headings get `id`s for deep links.
 *
 * Raw HTML in Markdown is escaped and unsafe URLs (e.g. `javascript:`) are dropped, so posts cannot
 * inject scripts and the site can run with a strict Content-Security-Policy.
 */
class MarkdownRenderer {
    private val extensions =
        listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            AutolinkExtension.create(),
            HeadingAnchorExtension.create(),
        )
    private val parser = Parser.builder().extensions(extensions).build()
    private val htmlRenderer =
        HtmlRenderer
            .builder()
            .extensions(extensions)
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build()
    private val textRenderer = TextContentRenderer.builder().build()

    fun render(markdown: String): RenderedMarkdown {
        val document = parser.parse(markdown)
        val text = textRenderer.render(document)
        val firstParagraph =
            generateSequence(document.firstChild, Node::getNext)
                .firstOrNull { it is Paragraph }
                ?.let { textRenderer.render(it).replace(WHITESPACE, " ").trim() }
                .orEmpty()
        return RenderedMarkdown(
            html = htmlRenderer.render(document),
            wordCount = text.split(WHITESPACE).count { it.isNotBlank() },
            firstParagraph = firstParagraph,
        )
    }
}

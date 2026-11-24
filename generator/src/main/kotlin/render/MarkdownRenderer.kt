package dimaruns.generator.render

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.text.TextContentRenderer

data class Heading(
    val id: String,
    val text: String,
)

data class RenderedMarkdown(
    val html: String,
    val wordCount: Int,
    val firstParagraph: String,
    val headings: List<Heading> = emptyList(),
)

private val WHITESPACE = Regex("\\s+")
private val H2 = Regex("""<h2 id="([^"]+)">(.*?)</h2>""")
private val TAG = Regex("<[^>]+>")

fun String.noBreakSub3(): String = replace("sub-3", "sub\u20113")

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
        val headings = mutableListOf<Heading>()
        val html =
            H2.replace(htmlRenderer.render(document).noBreakSub3()) { match ->
                val (id, inner) = match.destructured
                headings += Heading(id, unescapeHtml(inner.replace(TAG, "")))
                """<h2 id="$id">$inner <a class="anchor" href="#$id" aria-label="Link to this section">#</a></h2>"""
            }
        return RenderedMarkdown(
            html = html,
            wordCount = text.split(WHITESPACE).count { it.isNotBlank() },
            firstParagraph = firstParagraph,
            headings = headings,
        )
    }
}

private fun unescapeHtml(text: String): String =
    text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")

package dimaruns.generator.render

import dimaruns.generator.content.Post
import dimaruns.generator.site.SiteConfig
import kotlinx.datetime.LocalDate
import java.io.StringWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

private const val ATOM_NS = "http://www.w3.org/2005/Atom"
private const val SITEMAP_NS = "http://www.sitemaps.org/schemas/sitemap/0.9"
private const val FEED_SIZE = 20

/** Machine-readable outputs. Written with a streaming XML writer, so escaping is always correct. */
object Feeds {
    fun atom(
        site: SiteConfig,
        posts: List<Post>,
    ): String =
        xml {
            writeStartElement("feed")
            writeDefaultNamespace(ATOM_NS)
            element("title", site.title)
            element("subtitle", site.description)
            link(site.absolute("/feed.xml"), rel = "self")
            link(site.absolute("/"), rel = "alternate")
            element("id", site.absolute("/"))
            // Derived from content rather than the clock, so identical content gives byte-identical output.
            element("updated", timestamp(posts.maxOfOrNull { it.meta.date } ?: LocalDate(1970, 1, 1)))
            writeStartElement("author")
            element("name", site.author)
            writeEndElement()

            posts.take(FEED_SIZE).forEach { post ->
                writeStartElement("entry")
                element("title", post.meta.title)
                link(site.absolute(post.path), rel = "alternate")
                element("id", site.absolute(post.path))
                element("published", timestamp(post.meta.date))
                element("updated", timestamp(post.meta.date))
                element("summary", post.excerpt)
                post.meta.tags.forEach { tag ->
                    writeEmptyElement("category")
                    writeAttribute("term", tag)
                }
                writeStartElement("content")
                writeAttribute("type", "html")
                writeCharacters(post.html)
                writeEndElement()
                writeEndElement()
            }
            writeEndElement()
        }

    fun sitemap(
        site: SiteConfig,
        posts: List<Post>,
        tags: Collection<String>,
    ): String =
        xml {
            writeStartElement("urlset")
            writeDefaultNamespace(SITEMAP_NS)
            url(site.absolute("/"), posts.maxOfOrNull { it.meta.date })
            posts.forEach { url(site.absolute(it.path), it.meta.date) }
            url(site.absolute("/about/"), null)
            url(site.absolute("/tags/"), null)
            tags.forEach { url(site.absolute(tagPath(it)), null) }
            writeEndElement()
        }

    private fun XMLStreamWriter.url(
        loc: String,
        lastModified: LocalDate?,
    ) {
        writeStartElement("url")
        element("loc", loc)
        lastModified?.let { element("lastmod", it.toString()) }
        writeEndElement()
    }

    private fun XMLStreamWriter.element(
        name: String,
        text: String,
    ) {
        writeStartElement(name)
        writeCharacters(text)
        writeEndElement()
    }

    private fun XMLStreamWriter.link(
        href: String,
        rel: String,
    ) {
        writeEmptyElement("link")
        writeAttribute("rel", rel)
        writeAttribute("href", href)
    }

    private fun timestamp(date: LocalDate): String = "${date}T00:00:00Z"

    private fun xml(block: XMLStreamWriter.() -> Unit): String {
        val out = StringWriter()
        val writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out)
        writer.writeStartDocument("UTF-8", "1.0")
        writer.block()
        writer.writeEndDocument()
        writer.close()
        return out.toString() + "\n"
    }
}

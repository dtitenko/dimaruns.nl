package dimaruns.generator.site

import dimaruns.generator.TestContent
import dimaruns.generator.content.ContentException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.walk
import kotlin.io.path.writeText

class SiteBuilderTest :
    FunSpec({
        fun content() =
            TestContent()
                .site()
                .post("first.md", "title: First Post\ndate: 2026-01-01\ntags: [kotlin, edge]")
                .post("second.md", "title: Second <Post> & more\ndate: 2026-02-01\ntags: [kotlin]")
                .post("draft.md", "title: Secret Draft\ndate: 2026-03-01\ndraft: true")

        // Mirrors the assets that the templates link to.
        fun staticFiles(): Path {
            val static = Files.createTempDirectory("static")
            (static / "assets").createDirectories()
            (static / "assets" / "style.css").writeText("body{}")
            (static / "favicon.svg").writeText("<svg/>")
            (static / "assets" / "views.js").writeText("")
            (static / "assets" / "icons.svg").writeText("<svg/>")
            (static / "assets" / "theme.js").writeText("")
            return static
        }

        fun build(
            content: TestContent,
            out: Path = Files.createTempDirectory("site") / "out",
            static: Path? = staticFiles(),
            drafts: Boolean = false,
            baseUrl: String? = null,
        ): Path {
            SiteBuilder().build(BuildOptions(content.root, static, out, drafts, baseUrl))
            return out
        }

        test("renders all pages, feeds and static files") {
            val out = build(content())

            listOf(
                "index.html",
                "posts/first-post/index.html",
                "posts/second-post-more/index.html",
                "tags/index.html",
                "tags/kotlin/index.html",
                "tags/edge/index.html",
                "404.html",
                "feed.xml",
                "sitemap.xml",
                "robots.txt",
                "assets/style.css",
            ).forEach { (out / it).shouldExist() }
            (out / "posts/secret-draft").shouldNotExist()
        }

        test("every internal link points to a generated file") {
            val out = build(content())
            val brokenLinks =
                out
                    .walk()
                    .filter { it.extension == "html" }
                    .flatMap { page -> internalLinks(page.readText()).map { page to it } }
                    .filterNot { (_, link) -> resolve(out, link).isRegularFile() }
                    .map { (page, link) -> "${out.relativize(page)} -> $link" }
                    .toList()

            brokenLinks.shouldBeEmpty()
        }

        test("the real icon sprite parses and defines every icon the pages use") {
            // Path passed in by generator/build.gradle.kts, which also declares it as a test input.
            val sprite = parseXml(Path.of(System.getProperty("iconSprite")))
            val symbols = sprite.getElementsByTagName("symbol")
            val defined = (0 until symbols.length).map { (symbols.item(it) as Element).getAttribute("id") }.toSet()
            val used =
                build(content())
                    .walk()
                    .filter { it.extension == "html" }
                    .flatMap { page -> ICON.findAll(page.readText()).map { it.groupValues[1] } }
                    .toSet()

            (used - defined).shouldBeEmpty()
        }

        test("escapes titles in HTML and produces well-formed feeds") {
            val out = build(content())

            (out / "index.html").readText() shouldContain "Second &lt;Post&gt; &amp; more"
            val feed = parseXml(out / "feed.xml")
            feed.getElementsByTagName("entry").length shouldBe 2
            feed.getElementsByTagName("title").item(1).textContent shouldBe "Second <Post> & more"
            parseXml(out / "sitemap.xml").getElementsByTagName("url").length shouldBe 6
        }

        test("base URL override is used for absolute URLs only") {
            val out = build(content(), baseUrl = "https://preview.example.dev")
            val html = (out / "posts/first-post/index.html").readText()

            html shouldContain "<link href=\"https://preview.example.dev/posts/first-post/\" rel=\"canonical\">"
            html shouldNotContain "example.test"
            (out / "robots.txt").readText() shouldContain "https://preview.example.dev/sitemap.xml"
        }

        test("drafts are rendered and marked when requested") {
            val out = build(content(), drafts = true)
            (out / "posts/secret-draft/index.html").readText() shouldContain "<strong>Draft</strong>"
        }

        test("asset URLs carry a content hash that changes with the file") {
            val static = staticFiles()

            fun styleHref(out: Path) =
                Regex("""href="(/assets/style\.css\?v=[0-9a-f]{10})"""")
                    .find((out / "index.html").readText())
                    ?.groupValues
                    ?.get(1)

            val before = styleHref(build(content(), static = static))
            (static / "assets" / "style.css").writeText("body{color:red}")
            val after = styleHref(build(content(), static = static))

            withClue("before=$before after=$after") {
                (before != null && after != null && before != after) shouldBe true
            }
        }

        test("stale files from a previous build are removed") {
            val out = Files.createTempDirectory("site") / "out"
            out.createDirectories()
            (out / "stale.html").writeText("old")

            build(content(), out = out)

            (out / "stale.html").shouldNotExist()
        }

        test("refuses to wipe an output directory that contains the content") {
            val content = content()
            shouldThrow<IllegalArgumentException> { build(content, out = content.root.parent) }
        }

        test("invalid site.yaml is reported as a content problem") {
            val content = TestContent().site(baseUrl = "not-a-url")
            val problem = shouldThrow<ContentException> { build(content) }.problems.single()

            withClue(problem) {
                problem.source shouldBe "site.yaml"
                problem.message shouldContain "baseUrl"
            }
        }
    })

private val HREF = Regex("""(?:href|src)="(/[^"#?]*)""")

private val ICON = Regex("""icons\.svg[^"#]*#([\w-]+)""")

private fun internalLinks(html: String): List<String> = HREF.findAll(html).map { it.groupValues[1] }.toList()

private fun resolve(
    root: Path,
    link: String,
): Path = root.resolve(link.removePrefix("/")).let { if (link.endsWith("/")) it / "index.html" else it }

private fun parseXml(file: Path): Document =
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile())

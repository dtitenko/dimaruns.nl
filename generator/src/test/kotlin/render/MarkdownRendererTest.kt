package dimaruns.generator.render

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class MarkdownRendererTest :
    FunSpec({
        val renderer = MarkdownRenderer()

        test("escapes raw HTML so posts cannot inject scripts") {
            val html = renderer.render("Hello <script>alert(1)</script>").html
            html shouldNotContain "<script>"
            html shouldContain "&lt;script&gt;"
        }

        test("drops javascript: URLs") {
            renderer.render("[click](javascript:alert(1))").html shouldNotContain "javascript:"
        }

        test("renders GFM tables, strikethrough and autolinks") {
            val html = renderer.render("| a | b |\n|---|---|\n| 1 | 2 |\n\n~~old~~ https://example.com").html
            html shouldContain "<table>"
            html shouldContain "<del>old</del>"
            html shouldContain "href=\"https://example.com\">https://example.com</a>"
        }

        test("gives headings ids for deep links") {
            renderer.render("## Getting started").html shouldContain "<h2 id=\"getting-started\">"
        }

        test("lists h2 headings and adds a self-link anchor to each") {
            val rendered = renderer.render("## Carbs & water\n\nText.\n\n### Detail\n\n## Sleep")
            rendered.headings shouldBe listOf(Heading("carbs--water", "Carbs & water"), Heading("sleep", "Sleep"))
            rendered.html shouldContain
                "<a class=\"anchor\" href=\"#sleep\" aria-label=\"Link to this section\">#</a></h2>"
        }

        test("keeps sub-3 from breaking across lines") {
            renderer.render("A sub-3 race").html shouldContain "sub\u20113"
        }

        test("keeps language classes on fenced code blocks") {
            renderer.render("```kotlin\nval x = 1\n```").html shouldContain "class=\"language-kotlin\""
        }

        test("counts words and extracts the first paragraph as plain text") {
            val rendered = renderer.render("# Title\n\nFirst *para*\ngraph here.\n\nSecond one.")
            rendered.firstParagraph shouldBe "First para graph here."
            rendered.wordCount shouldBe 7
        }
    })

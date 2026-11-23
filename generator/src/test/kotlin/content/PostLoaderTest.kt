package dimaruns.generator.content

import dimaruns.generator.TestContent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith

class PostLoaderTest :
    FunSpec({
        val loader = PostLoader()

        test("loads posts newest first and derives slug, excerpt and reading time") {
            val content =
                TestContent()
                    .post("a.md", "title: Older Post\ndate: 2026-01-01")
                    .post("b.md", "title: Newer Post\ndate: 2026-02-01\nslug: custom", body = "word ".repeat(450))

            val posts = loader.load(content.posts, includeDrafts = false)

            posts.map { it.slug.value } shouldContainExactly listOf("custom", "older-post")
            posts[0].readingMinutes shouldBe 3
            posts[0].excerpt shouldEndWith "…"
            posts[1].excerpt shouldBe "Some body text."
            posts[1].path shouldBe "/posts/older-post/"
        }

        test("an explicit summary wins over the first paragraph") {
            val content = TestContent().post("a.md", "title: A\ndate: 2026-01-01\nsummary: Short.")
            loader.load(content.posts, includeDrafts = false).single().excerpt shouldBe "Short."
        }

        test("drafts are excluded unless requested") {
            val content =
                TestContent()
                    .post("a.md", "title: Published\ndate: 2026-01-01")
                    .post("b.md", "title: Draft\ndate: 2026-01-02\ndraft: true")

            loader.load(content.posts, includeDrafts = false) shouldHaveSize 1
            loader.load(content.posts, includeDrafts = true) shouldHaveSize 2
        }

        test("reports every problem across all files at once") {
            val content =
                TestContent()
                    .post("no-date.md", "title: Missing date")
                    .post("bad-tag.md", "title: Bad tag\ndate: 2026-01-01\ntags: [Kotlin]")
                    .post("bad-date.md", "title: Bad date\ndate: 2026-13-45")
                    .post("unknown-key.md", "title: Typo\ndate: 2026-01-01\ntitel: oops")
                    .post("ok.md", "title: Fine\ndate: 2026-01-01")

            val problems = shouldThrow<ContentException> { loader.load(content.posts, false) }.problems

            problems.map { it.source }.toSet() shouldBe
                setOf("no-date.md", "bad-tag.md", "bad-date.md", "unknown-key.md")
            problems.single { it.source == "bad-tag.md" }.message shouldContain "lowercase-kebab-case"
        }

        test("rejects two posts with the same slug") {
            val content =
                TestContent()
                    .post("a.md", "title: Same\ndate: 2026-01-01")
                    .post("b.md", "title: Other\ndate: 2026-01-02\nslug: same")

            shouldThrow<ContentException> { loader.load(content.posts, false) }
                .problems
                .single()
                .message shouldBe "duplicate slug 'same'"
        }
    })

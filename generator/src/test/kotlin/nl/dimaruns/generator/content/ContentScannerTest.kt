package nl.dimaruns.generator.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText

class ContentScannerTest :
    FunSpec({
        test("finds only markdown files, sorted by relative path") {
            val dir = Files.createTempDirectory("content")
            (dir / "b.md").writeText("b")
            (dir / "notes.txt").writeText("ignored")
            (dir / "2026").createDirectories()
            (dir / "2026" / "a.md").writeText("a")

            findPosts(dir).map { dir.relativize(it).toString().replace('\\', '/') } shouldBe
                listOf("2026/a.md", "b.md")
        }

        test("missing directory yields no posts") {
            findPosts(Files.createTempDirectory("x") / "missing").shouldBeEmpty()
        }
    })

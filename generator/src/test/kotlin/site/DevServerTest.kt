package dimaruns.generator.site

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import kotlin.io.path.deleteExisting
import kotlin.io.path.div
import kotlin.io.path.writeText

class DevServerTest :
    FunSpec({
        test("injects the reload script before </body>") {
            injectReload("<html><body><p>hi</p></body></html>") shouldContain "</script></body>"
        }

        test("snapshot changes when a file is added, edited or removed") {
            val dir = Files.createTempDirectory("dev")
            val start = snapshot(listOf(dir))
            val file = dir / "a.md"
            file.writeText("one")
            val added = snapshot(listOf(dir))
            added shouldNotBe start
            Files.setLastModifiedTime(file, FileTime.fromMillis(0))
            snapshot(listOf(dir)) shouldNotBe added
            file.deleteExisting()
            snapshot(listOf(dir)) shouldBe start
        }
    })

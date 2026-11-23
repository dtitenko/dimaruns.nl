package dimaruns.generator

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText

/** Builds a throw-away content directory for tests. */
class TestContent(
    val root: Path = Files.createTempDirectory("content"),
) {
    val posts: Path = (root / "posts").createDirectories()

    fun site(baseUrl: String = "https://example.test") =
        apply {
            (root / "site.yaml").writeText(
                """
                title: Test Site
                description: A site for tests
                author: Tester
                baseUrl: $baseUrl
                """.trimIndent(),
            )
        }

    fun post(
        file: String,
        frontMatter: String,
        body: String = "Some body text.",
    ) = apply {
        (posts / file).writeText("---\n${frontMatter.trimIndent()}\n---\n$body\n")
    }
}

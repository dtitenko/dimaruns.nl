package nl.dimaruns.generator.content

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

/** Finds Markdown sources under a content directory, in a stable order for reproducible builds. */
fun findPosts(contentDir: Path): List<Path> {
    if (!contentDir.isDirectory()) return emptyList()
    return contentDir
        .walk()
        .filter { it.isRegularFile() && it.extension == "md" }
        .sortedBy { contentDir.relativize(it).toString().replace('\\', '/') }
        .toList()
}

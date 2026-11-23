package nl.dimaruns.generator

import kotlin.io.path.Path

/** Entry point of the static-site generator. Rendering arrives in phase 2; for now it lists content. */
fun main(args: Array<String>) {
    val contentDir = Path(args.firstOrNull() ?: "content/posts")
    val posts = findPosts(contentDir)
    println("Found ${posts.size} post(s) in $contentDir")
    posts.forEach { println("  - ${contentDir.relativize(it)}") }
}

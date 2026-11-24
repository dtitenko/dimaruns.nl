package dimaruns.generator.site

import dimaruns.generator.content.PostLoader
import dimaruns.generator.render.Feeds
import dimaruns.generator.render.Templates
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.walk
import kotlin.io.path.writeText

data class BuildOptions(
    /** Directory containing `site.yaml` and `posts/`. */
    val contentRoot: Path,
    /** Files copied verbatim into the output (CSS, favicon, ...). Optional. */
    val staticDir: Path?,
    val outDir: Path,
    val includeDrafts: Boolean = false,
    val baseUrlOverride: String? = null,
)

private fun contentHash(file: Path): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(file.readBytes())
        .joinToString("") { "%02x".format(it) }
        .take(10)

data class BuildReport(
    val posts: Int,
    val tags: Int,
    val files: Int,
)

/** Renders the whole site. Output is fully regenerated on every build, so stale pages never linger. */
class SiteBuilder(
    private val loader: PostLoader = PostLoader(),
) {
    @OptIn(ExperimentalPathApi::class)
    fun build(options: BuildOptions): BuildReport {
        val site = SiteConfig.load(options.contentRoot / "site.yaml", options.baseUrlOverride)
        val posts = loader.load(options.contentRoot / "posts", options.includeDrafts)
        val tags =
            posts
                .flatMap { post -> post.meta.tags.map { it to post } }
                .groupBy({ it.first }, { it.second })
                .toSortedMap()

        val out = options.outDir.toAbsolutePath().normalize()
        val contentRoot = options.contentRoot.toAbsolutePath().normalize()
        require(!contentRoot.startsWith(out)) { "Refusing to clean $out: it contains the content directory" }
        if (out.exists()) out.deleteRecursively()
        out.createDirectories()
        options.staticDir?.takeIf { it.isDirectory() }?.copyToRecursively(out, followLinks = false, overwrite = true)

        // Cache busting: assets are linked as /path?v=<content hash>, so a changed file gets a new URL
        // and browsers fetch it at once instead of serving a stale cached copy.
        val templates =
            Templates(site, recent = posts.take(5), allTags = tags) { path ->
                val file = out.resolve(path.removePrefix("/"))
                if (file.isRegularFile()) "$path?v=${contentHash(file)}" else path
            }

        fun write(
            relativePath: String,
            content: String,
        ) {
            val file = out / relativePath
            file.parent.createDirectories()
            file.writeText(content)
        }

        write("index.html", templates.index(posts))
        posts.forEach { write("posts/${it.slug}/index.html", templates.post(it)) }
        write("tags/index.html", templates.tagIndex(tags))
        tags.forEach { (tag, tagged) -> write("tags/$tag/index.html", templates.tag(tag, tagged)) }
        write("404.html", templates.notFound())
        write("feed.xml", Feeds.atom(site, posts))
        write("sitemap.xml", Feeds.sitemap(site, posts, tags.keys))
        write("robots.txt", "User-agent: *\nAllow: /\n\nSitemap: ${site.absolute("/sitemap.xml")}\n")

        return BuildReport(posts = posts.size, tags = tags.size, files = out.walk().count { it.isRegularFile() })
    }
}

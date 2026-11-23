package nl.dimaruns.generator.render

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import nl.dimaruns.generator.content.Post
import nl.dimaruns.generator.site.SiteConfig
import java.time.format.DateTimeFormatter
import java.util.Locale

fun tagPath(tag: String): String = "/tags/$tag/"

/**
 * Type-safe HTML templates. Output contains no inline scripts or styles, so it works with a strict CSP.
 * Pages use root-relative links; absolute URLs appear only where the spec requires them (canonical, Open Graph).
 */
class Templates(
    private val site: SiteConfig,
    /** Maps an asset path to the URL to link, e.g. with a cache-busting version. */
    private val asset: (String) -> String = { it },
) {
    private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

    fun index(posts: List<Post>): String =
        page(title = null, description = site.description, path = "/") {
            h1("visually-hidden") { +site.title }
            postList(posts)
        }

    fun post(post: Post): String =
        page(title = post.meta.title, description = post.excerpt, path = post.path, type = "article") {
            article("post") {
                header {
                    h1 { +post.meta.title }
                    postMeta(post, showViews = true)
                }
                div("post-body") { unsafe { +post.html } }
            }
            // External module script (not inline) so the strict CSP in static/_headers applies.
            script(type = "module", src = asset("/assets/views.js")) {}
        }

    fun tagIndex(tags: Map<String, List<Post>>): String =
        page(title = "Tags", description = "All tags on ${site.title}", path = "/tags/") {
            h1 { +"Tags" }
            if (tags.isEmpty()) {
                p { +"No tags yet." }
            } else {
                ul("tag-list") {
                    tags.forEach { (tag, posts) ->
                        li {
                            a(tagPath(tag), classes = "tag") { +"#$tag" }
                            +" (${posts.size})"
                        }
                    }
                }
            }
        }

    fun tag(
        tag: String,
        posts: List<Post>,
    ): String =
        page(title = "#$tag", description = "Posts tagged $tag on ${site.title}", path = tagPath(tag)) {
            h1 { +"Posts tagged #$tag" }
            postList(posts)
        }

    fun notFound(): String =
        page(title = "Page not found", description = "Page not found", path = "/404.html") {
            h1 { +"Page not found" }
            p {
                +"The page you are looking for does not exist. "
                a("/") { +"Go to the homepage" }
                +"."
            }
        }

    private fun page(
        title: String?,
        description: String,
        path: String,
        type: String = "website",
        content: MAIN.() -> Unit,
    ): String =
        "<!DOCTYPE html>\n" +
            createHTML(prettyPrint = false).html {
                attributes["lang"] = site.language
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1")
                    title(title?.let { "$it · ${site.title}" } ?: site.title)
                    meta(name = "description", content = description)
                    meta(name = "author", content = site.author)
                    link(href = site.absolute(path), rel = "canonical")
                    link(href = "/feed.xml", rel = "alternate", type = "application/atom+xml") {
                        attributes["title"] = site.title
                    }
                    link(href = asset("/assets/style.css"), rel = "stylesheet")
                    link(href = asset("/favicon.svg"), rel = "icon", type = "image/svg+xml")
                    openGraph("og:type", type)
                    openGraph("og:title", title ?: site.title)
                    openGraph("og:description", description)
                    openGraph("og:url", site.absolute(path))
                    openGraph("og:site_name", site.title)
                }
                body {
                    a("#main", classes = "skip-link") { +"Skip to content" }
                    header("site-header") {
                        a("/", classes = "site-title") { +site.title }
                        nav {
                            attributes["aria-label"] = "Main"
                            ul {
                                li { a("/") { +"Blog" } }
                                li { a("/tags/") { +"Tags" } }
                                li { a("/feed.xml") { +"Feed" } }
                            }
                        }
                    }
                    main {
                        id = "main"
                        content()
                    }
                    footer("site-footer") {
                        p { +"© ${site.author}" }
                    }
                }
            }

    private fun HEAD.openGraph(
        property: String,
        value: String,
    ) = meta {
        attributes["property"] = property
        content = value
    }

    private fun FlowContent.postList(posts: List<Post>) {
        if (posts.isEmpty()) {
            p { +"No posts yet." }
            return
        }
        ol("post-list") {
            posts.forEach { post ->
                li {
                    article {
                        h2 { a(post.path) { +post.meta.title } }
                        postMeta(post)
                        p { +post.excerpt }
                    }
                }
            }
        }
    }

    private fun FlowContent.postMeta(
        post: Post,
        showViews: Boolean = false,
    ) {
        p("post-meta") {
            time {
                attributes["datetime"] = post.meta.date.toString()
                +format(post.meta.date)
            }
            +", ${post.readingMinutes} min read"
            // Filled in by /assets/views.js from the edge API.
            if (showViews) span { attributes["data-views"] = post.slug.value }
            if (post.meta.draft) {
                +", "
                strong { +"Draft" }
            }
            post.meta.tags.forEach { tag ->
                +" "
                a(tagPath(tag), classes = "tag") { +"#$tag" }
            }
        }
    }

    private fun format(date: LocalDate): String = date.toJavaLocalDate().format(dateFormat)
}

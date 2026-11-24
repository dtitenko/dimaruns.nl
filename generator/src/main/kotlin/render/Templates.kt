package dimaruns.generator.render

import dimaruns.generator.content.Post
import dimaruns.generator.site.SiteConfig
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.Locale

fun tagPath(tag: String): String = "/tags/$tag/"

private const val EMAIL = "d.titenko@gmail.com"

/**
 * Type-safe HTML templates. Output contains no inline scripts or styles, so it works with a strict CSP.
 * Pages use root-relative links; absolute URLs appear only where the spec requires them (canonical, Open Graph).
 */
class Templates(
    private val site: SiteConfig,
    /** Newest posts, listed in the sidebar of every page. */
    private val recent: List<Post> = emptyList(),
    /** All tags with their posts, listed in the sidebar of every page. */
    private val allTags: Map<String, List<Post>> = emptyMap(),
    /** Maps an asset path to the URL to link, e.g. with a cache-busting version. */
    private val asset: (String) -> String = { it },
) {
    private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

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
                    postMeta(post, full = true)
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
                    // Classic (blocking) script so the saved theme applies before first paint.
                    script(src = asset("/assets/theme.js")) {}
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
                        themeSwitcher()
                        nav {
                            attributes["aria-label"] = "Main"
                            ul {
                                li { a("/") { +"Blog" } }
                                li { a("/tags/") { +"Tags" } }
                                li { a("/feed.xml") { +"Feed" } }
                            }
                        }
                    }
                    div("layout") {
                        main {
                            id = "main"
                            content()
                        }
                        sidebar()
                    }
                    footer("site-footer") {
                        div("social") {
                            socialLinks(size = 32)
                            a("/feed.xml") {
                                attributes["title"] = "Feed"
                                attributes["aria-label"] = "Feed"
                                icon("rss", 32)
                            }
                        }
                        p { +"© ${Year.now()} ${site.author}" }
                    }
                }
            }

    private fun FlowContent.sidebar() =
        aside("sidebar") {
            attributes["aria-label"] = "About"
            section("bio") {
                hr("hr-slash")
                img(alt = site.author, src = asset("/assets/avatar.jpg"), classes = "bio-photo")
                hr("hr-slash")
                p("bio-name") { +site.author }
                p("bio-desc") { +"Runner and software engineer" }
                p("bio-info") {
                    icon("location", 20)
                    +"The Hague, The Netherlands"
                }
                p("bio-info") {
                    icon("email", 20)
                    a("mailto:$EMAIL") { +EMAIL }
                }
                p("bio-info") {
                    icon("link", 20)
                    a(site.baseUrl) { +site.baseUrl }
                }
                div("social") { socialLinks(size = 25) }
            }
            hr("hr-fade")
            if (recent.isNotEmpty()) {
                section("sidebar-recent") {
                    h2 { +"Posts" }
                    ul { recent.forEach { li { a(it.path) { +it.meta.title } } } }
                }
            }
            if (allTags.isNotEmpty()) {
                section("sidebar-tags") {
                    h2 { +"Tags" }
                    allTags.forEach { (tag, posts) ->
                        span("tag-count") {
                            a(tagPath(tag), classes = "tag") { +tag }
                            +" ${posts.size}"
                        }
                        +" "
                    }
                }
            }
        }

    /** Theme picker driven by /assets/theme.js; the theme names match the palettes in style.css. */
    private fun FlowContent.themeSwitcher() =
        div("theme-switcher") {
            button(type = ButtonType.button) {
                attributes["aria-label"] = "Select theme"
                attributes["aria-expanded"] = "false"
                icon("theme", 22)
            }
            ul {
                listOf("dark", "light", "hacker", "solarized", "kimbie").forEach { theme ->
                    li {
                        button(type = ButtonType.button) {
                            attributes["data-set-theme"] = theme
                            attributes["aria-pressed"] = (theme == "dark").toString()
                            +theme
                        }
                    }
                }
            }
        }

    private fun FlowContent.socialLinks(size: Int) {
        listOf(
            "Strava" to "https://www.strava.com/athletes/33489329",
            "GitHub" to "https://github.com/dtitenko/",
            "LinkedIn" to "https://www.linkedin.com/in/dtitenko/",
            "Email" to "mailto:$EMAIL",
        ).forEach { (name, url) ->
            a(url) {
                if (url.startsWith("http")) {
                    target = "_blank"
                    rel = "noopener noreferrer"
                }
                attributes["title"] = name
                attributes["aria-label"] = name
                icon(name.lowercase(), size)
            }
        }
    }

    /** A decorative icon from the /assets/icons.svg sprite, drawn in currentColor. */
    private fun HTMLTag.icon(
        id: String,
        size: Int,
    ) = unsafe {
        +"""<svg width="$size" height="$size" aria-hidden="true"><use href="${asset(
            "/assets/icons.svg",
        )}#$id"/></svg>"""
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

    /** Date and reading time as on the reference design; the post page adds views, draft flag and tags. */
    private fun FlowContent.postMeta(
        post: Post,
        full: Boolean = false,
    ) {
        p("post-meta") {
            time {
                attributes["datetime"] = post.meta.date.toString()
                +"📅 ${format(post.meta.date)}"
            }
            +" · ☕ ${post.readingMinutes} min read"
            // Filled in by /assets/views.js from the edge API.
            if (full) span { attributes["data-views"] = post.slug.value }
            if (post.meta.draft) {
                +", "
                strong { +"Draft" }
            }
            if (full) {
                post.meta.tags.forEach { tag ->
                    +" "
                    a(tagPath(tag), classes = "tag") { +"#$tag" }
                }
            }
        }
    }

    private fun format(date: LocalDate): String = date.toJavaLocalDate().format(dateFormat)
}

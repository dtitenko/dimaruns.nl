package dimaruns.generator.render

import dimaruns.generator.content.Post
import dimaruns.generator.site.SiteConfig
import dimaruns.shared.RaceResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.net.URLEncoder
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.Locale

fun tagPath(tag: String): String = "/tags/$tag/"

private const val EMAIL = "d.titenko@gmail.com"
private const val STRAVA = "https://www.strava.com/athletes/33489329"
private const val DEFAULT_IMAGE = "/assets/og-default.jpg"
private const val BIO_ROLE = "Runner and software engineer, The Hague"

/**
 * Type-safe HTML templates. Output contains no inline scripts or styles, so it works with a strict CSP.
 * Pages use root-relative links; absolute URLs appear only where the spec requires them (canonical, Open Graph).
 */
class Templates(
    private val site: SiteConfig,
    /** Maps an asset path to the URL to link, e.g. with a cache-busting version. */
    private val asset: (String) -> String = { it },
) {
    private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    fun index(posts: List<Post>): String =
        page(title = null, description = site.description, path = "/") {
            h1("visually-hidden") { +site.title }
            section("intro") {
                p("lede") {
                    +"I’m Dima, a software engineer in The Hague. I write about chasing a sub\u20113 marathon."
                }
                byline()
            }
            postList(posts)
        }

    fun post(
        post: Post,
        newer: Post? = null,
        older: Post? = null,
    ): String {
        val title = post.meta.title.noBreakSub3()
        return page(
            title = post.meta.title,
            description = post.excerpt,
            path = post.path,
            type = "article",
            image = post.meta.image,
            jsonLd = articleJsonLd(post),
        ) {
            article("post") {
                attributes["data-views"] = post.slug.value
                header("hero") {
                    postMeta(post)
                    h1 { +title }
                    p("dek") { +post.excerpt.noBreakSub3() }
                    byline()
                }
                if (post.meta.results.isNotEmpty()) results(post.meta.results)
                div("post-main") {
                    if (post.meta.numbered && post.headings.isNotEmpty()) {
                        details("toc-mobile") {
                            summary { +"Jump to a lesson (${post.headings.size})" }
                            tocList(post)
                        }
                    }
                    div(if (post.meta.numbered) "post-body numbered" else "post-body") { unsafe { +post.html } }
                    endCard(post)
                    if (newer != null || older != null) {
                        nav("post-nav") {
                            attributes["aria-label"] = "More posts"
                            older?.let { neighbour("Previous post", it) }
                            newer?.let { neighbour("Next post", it) }
                        }
                    }
                }
                if (post.meta.numbered && post.headings.isNotEmpty()) {
                    aside("post-aside") {
                        nav("toc") {
                            attributes["aria-labelledby"] = "toc-label"
                            p("toc-label") {
                                id = "toc-label"
                                +"${post.headings.size} lessons"
                            }
                            tocList(post)
                            div("progress") { span {} }
                        }
                    }
                }
            }
            // External module scripts (not inline) so the strict CSP in static/_headers applies.
            script(type = "module", src = asset("/assets/views.js")) {}
            if (post.meta.numbered) script(type = "module", src = asset("/assets/post.js")) {}
        }
    }

    fun tagIndex(tags: Map<String, List<Post>>): String =
        page(title = "Tags", description = "All tags on ${site.title}", path = "/tags/") {
            h1("page-title") { +"Tags" }
            if (tags.isEmpty()) {
                p { +"No tags yet." }
            } else {
                ul("tag-list") {
                    tags.forEach { (tag, posts) ->
                        li {
                            a(tagPath(tag), classes = "tag") { +tag }
                            span("count") { +"${posts.size}" }
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
            h1("page-title") { +"Posts tagged $tag" }
            postList(posts)
        }

    fun about(): String =
        page(title = "About", description = "About ${site.author}", path = "/about/") {
            div("about") {
                h1("page-title") { +"About" }
                img(alt = "", src = asset("/assets/avatar.jpg"), classes = "avatar avatar-lg") {
                    width = "120"
                    height = "120"
                }
                p("lede") {
                    +"I’m Dima (${site.author}), a runner and software engineer in The Hague, The Netherlands. "
                    +"I write about chasing a sub\u20113 marathon and what I learn along the way, mostly the hard way."
                }
                ul("contact-list") {
                    contact("strava", "Strava", STRAVA)
                    contact("github", "GitHub", "https://github.com/dtitenko/")
                    contact("linkedin", "LinkedIn", "https://www.linkedin.com/in/dtitenko/")
                    contact("email", EMAIL, "mailto:$EMAIL")
                    contact("rss", "RSS feed", "/feed.xml")
                }
            }
        }

    fun notFound(): String =
        page(title = "Page not found", description = "Page not found", path = "/404.html") {
            h1("page-title") { +"Page not found" }
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
        image: String? = null,
        jsonLd: String? = null,
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
                    themeColor("light", "#F6F4EF")
                    themeColor("dark", "#161517")
                    link(href = site.absolute(path), rel = "canonical")
                    link(href = "/feed.xml", rel = "alternate", type = "application/atom+xml") {
                        attributes["title"] = site.title
                    }
                    preloadFont("/assets/fonts/barlow-condensed-700-normal.woff2")
                    preloadFont("/assets/fonts/source-serif-4-400-normal.woff2")
                    link(href = asset("/assets/style.css"), rel = "stylesheet")
                    link(href = asset("/favicon.svg"), rel = "icon", type = "image/svg+xml")
                    // Classic (blocking) script so the saved theme applies before first paint.
                    script(src = asset("/assets/theme.js")) {}
                    openGraph("og:type", type)
                    openGraph("og:title", title ?: site.title)
                    openGraph("og:description", description)
                    openGraph("og:url", site.absolute(path))
                    openGraph("og:site_name", site.title)
                    openGraph("og:image", site.absolute(image ?: DEFAULT_IMAGE))
                    if (image == null) {
                        openGraph("og:image:width", "1200")
                        openGraph("og:image:height", "630")
                    }
                    meta(name = "twitter:card", content = "summary_large_image")
                    jsonLd?.let { json ->
                        script(type = "application/ld+json") { unsafe { +json } }
                    }
                }
                body {
                    a("#main", classes = "skip-link") { +"Skip to content" }
                    header("site-header") {
                        div("frame") {
                            a("/", classes = "wordmark") {
                                span("lanes") {
                                    attributes["aria-hidden"] = "true"
                                    repeat(4) { i {} }
                                }
                                +site.title
                            }
                            nav("site-nav") {
                                attributes["aria-label"] = "Main"
                                ul {
                                    navItem("/", "Writing", path == "/" || path.startsWith("/posts/"))
                                    navItem("/tags/", "Tags", path.startsWith("/tags/"))
                                    navItem("/about/", "About", path == "/about/")
                                    navItem("/feed.xml", "RSS", false)
                                }
                            }
                            themeSwitcher()
                        }
                    }
                    main("frame") {
                        id = "main"
                        content()
                    }
                    footer("site-footer") {
                        div("frame") {
                            p { +"© ${Year.now()} ${site.author}" }
                            div("social") { socialLinks() }
                        }
                    }
                }
            }

    private fun UL.navItem(
        href: String,
        label: String,
        current: Boolean,
    ) = li {
        a(href) {
            if (current) attributes["aria-current"] = "page"
            +label
        }
    }

    private fun FlowContent.byline() =
        div("byline") {
            img(alt = "", src = asset("/assets/avatar.jpg"), classes = "avatar") {
                width = "44"
                height = "44"
            }
            div {
                p("byline-name") { +site.author }
                p("byline-role") { +BIO_ROLE }
            }
        }

    private fun FlowContent.results(
        results: List<RaceResult>,
        compact: Boolean = false,
    ) = div(if (compact) "results compact" else "results") {
        results.forEach { result ->
            div(if (result.win) "result win" else "result") {
                p("result-race") { +"${result.race} · ${result.date}" }
                p("result-time") { +result.time }
                if (result.note.isNotBlank()) p("result-note") { +result.note.noBreakSub3() }
            }
        }
    }

    private fun FlowContent.tocList(post: Post) =
        ol("toc-list") {
            post.headings.forEachIndexed { index, heading ->
                li {
                    a("#${heading.id}") {
                        span("toc-num") { +"%02d".format(index + 1) }
                        +heading.text.noBreakSub3()
                    }
                }
            }
        }

    private fun FlowContent.endCard(post: Post) =
        section("endcard") {
            attributes["aria-labelledby"] = "endcard-title"
            img(alt = "", src = asset("/assets/avatar.jpg"), classes = "avatar avatar-lg") {
                width = "72"
                height = "72"
            }
            div {
                h2 {
                    id = "endcard-title"
                    +"Thanks for reading"
                }
                p("endcard-bio") {
                    +"I’m Dima, a runner and software engineer in The Hague. "
                    +"I write about marathon training and what I learn the hard way."
                }
                div("actions") {
                    a(STRAVA, classes = "btn btn-primary") {
                        target = "_blank"
                        rel = "noopener noreferrer"
                        +"Follow on Strava"
                    }
                    a("/feed.xml", classes = "btn") { +"Subscribe via RSS" }
                    a("mailto:$EMAIL?subject=${mailtoEncode("Re: ${post.meta.title}")}", classes = "btn") {
                        +"Reply by email"
                    }
                }
            }
        }

    private fun FlowContent.neighbour(
        label: String,
        post: Post,
    ) = a(post.path, classes = "neighbour") {
        span("neighbour-label") { +label }
        span("neighbour-title") { +post.meta.title.noBreakSub3() }
    }

    private fun UL.contact(
        icon: String,
        label: String,
        url: String,
    ) = li {
        a(url) {
            if (url.startsWith("http")) {
                target = "_blank"
                rel = "noopener noreferrer"
            }
            icon(icon, 20)
            +label
        }
    }

    private fun FlowContent.themeSwitcher() =
        div("theme-switcher") {
            button(type = ButtonType.button) {
                attributes["aria-label"] = "Theme: System"
                attributes["aria-expanded"] = "false"
                attributes["aria-haspopup"] = "true"
                icon("theme", 22)
            }
            ul {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (theme, label) ->
                    li {
                        button(type = ButtonType.button) {
                            attributes["data-set-theme"] = theme
                            attributes["aria-pressed"] = (theme == "system").toString()
                            +label
                        }
                    }
                }
            }
        }

    private fun FlowContent.socialLinks() {
        listOf(
            "Strava" to STRAVA,
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
                icon(name.lowercase(), 22)
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

    private fun HEAD.themeColor(
        scheme: String,
        color: String,
    ) = meta(name = "theme-color", content = color) {
        attributes["media"] = "(prefers-color-scheme: $scheme)"
    }

    private fun HEAD.preloadFont(href: String) =
        link(href = href, rel = "preload") {
            attributes["as"] = "font"
            attributes["type"] = "font/woff2"
            attributes["crossorigin"] = ""
        }

    private fun FlowContent.postList(posts: List<Post>) {
        if (posts.isEmpty()) {
            p { +"No posts yet." }
            return
        }
        ol("post-list") {
            posts.forEach { post ->
                li {
                    article("card") {
                        postMeta(post)
                        h2 { a(post.path, classes = "card-link") { +post.meta.title.noBreakSub3() } }
                        p("dek") { +post.excerpt.noBreakSub3() }
                        if (post.meta.results.isNotEmpty()) results(post.meta.results, compact = true)
                    }
                }
            }
        }
    }

    private fun FlowContent.postMeta(post: Post) {
        p("kicker") {
            post.meta.tags.forEach { tag ->
                a(tagPath(tag), classes = "tag") { +tag }
                span("sep") { +" · " }
            }
            +"${post.readingMinutes} min read"
            span("sep") { +" · " }
            time {
                attributes["datetime"] = post.meta.date.toString()
                +format(post.meta.date)
            }
            if (post.meta.draft) {
                span("sep") { +" · " }
                strong { +"Draft" }
            }
        }
    }

    private fun articleJsonLd(post: Post): String {
        val url = site.absolute(post.path)
        val author = mapOf("@type" to "Person", "name" to site.author, "url" to site.absolute("/about/"))
        return json(
            mapOf(
                "@context" to "https://schema.org",
                "@type" to "Article",
                "headline" to post.meta.title,
                "description" to post.excerpt,
                "datePublished" to post.meta.date.toString(),
                "dateModified" to post.meta.date.toString(),
                "author" to author,
                "image" to site.absolute(post.meta.image ?: DEFAULT_IMAGE),
                "mainEntityOfPage" to mapOf("@type" to "WebPage", "@id" to url),
            ),
        )
    }

    private fun format(date: LocalDate): String = date.toJavaLocalDate().format(dateFormat)
}

private fun mailtoEncode(text: String): String = URLEncoder.encode(text, Charsets.UTF_8).replace("+", "%20")

internal fun json(value: Any?): String =
    when (value) {
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") { (k, v) -> json(k.toString()) + ":" + json(v) }
        is String ->
            buildString {
                append('"')
                value.forEach { c ->
                    when {
                        c == '"' -> append("\\\"")
                        c == '\\' -> append("\\\\")
                        c == '<' -> append("\\u003c")
                        c < ' ' -> append("\\u%04x".format(c.code))
                        else -> append(c)
                    }
                }
                append('"')
            }
        else -> error("Unsupported JSON value: $value")
    }

package dimaruns.edge

import dimaruns.shared.Slug

/** Storage for per-post view counts. */
interface ViewStore {
    suspend fun get(slug: Slug): Int

    /** Adds one view and returns the new total. */
    suspend fun increment(slug: Slug): Int
}

data class ApiResponse(
    val status: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap(),
)

/**
 * The `/api` routes. Plain Kotlin with no Workers types, so it is unit-tested on Node.
 *
 * - `GET  /api/health`       liveness check
 * - `GET  /api/views/{slug}` current count
 * - `POST /api/views/{slug}` count a view; only for posts that exist on the site
 */
class Api(
    private val views: ViewStore,
    private val postExists: suspend (Slug) -> Boolean,
) {
    suspend fun handle(
        method: String,
        path: String,
    ): ApiResponse {
        if (path == "/api/health") {
            return if (method == "GET") json(200, """{"status":"ok"}""") else methodNotAllowed("GET")
        }

        val rawSlug = path.removePrefix(VIEWS_PREFIX).takeIf { it != path } ?: return error(404, "not found")
        // The same validation the generator uses to name pages, shared through the KMP module.
        val slug = Slug.parse(rawSlug) ?: return error(400, "invalid slug")

        return when (method) {
            "GET" -> {
                viewsJson(slug, views.get(slug))
            }

            "POST" -> {
                // Without this check anyone could fill the database with made-up slugs.
                if (!postExists(slug)) error(404, "no such post") else viewsJson(slug, views.increment(slug))
            }

            else -> {
                methodNotAllowed("GET, POST")
            }
        }
    }

    private fun viewsJson(
        slug: Slug,
        count: Int,
    ) = json(200, """{"slug":"$slug","views":$count}""")

    private fun error(
        status: Int,
        message: String,
    ) = json(status, """{"error":"$message"}""")

    private fun methodNotAllowed(allow: String): ApiResponse {
        val response = error(405, "method not allowed")
        return response.copy(headers = response.headers + ("allow" to allow))
    }

    // Bodies only ever contain validated slugs, numbers and fixed messages, so no JSON escaping is needed.
    private fun json(
        status: Int,
        body: String,
    ) = ApiResponse(status, body, JSON_HEADERS)

    private companion object {
        const val VIEWS_PREFIX = "/api/views/"
        val JSON_HEADERS =
            mapOf(
                "content-type" to "application/json; charset=utf-8",
                "cache-control" to "no-store",
                "x-content-type-options" to "nosniff",
            )
    }
}

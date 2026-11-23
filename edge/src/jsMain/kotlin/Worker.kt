package dimaruns.edge

import dimaruns.shared.Slug
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.promise
import kotlin.js.Promise

private class D1ViewStore(
    private val db: D1Database,
) : ViewStore {
    override suspend fun get(slug: Slug): Int =
        db.prepare("SELECT count FROM views WHERE slug = ?").bind(slug.value).count()

    // One atomic statement: concurrent requests cannot lose an increment.
    override suspend fun increment(slug: Slug): Int =
        db
            .prepare(
                "INSERT INTO views (slug, count) VALUES (?, 1) " +
                    "ON CONFLICT (slug) DO UPDATE SET count = count + 1 RETURNING count",
            ).bind(slug.value)
            .count()

    private suspend fun D1PreparedStatement.count(): Int = (first("count").await() as Number?)?.toInt() ?: 0
}

/**
 * Entry point called from worker.mjs for every request under `/api/`. Other paths never reach the
 * Worker: Cloudflare serves the static site directly (`run_worker_first` in wrangler.jsonc).
 */
@OptIn(DelicateCoroutinesApi::class, ExperimentalJsExport::class)
@JsExport
fun handleFetch(
    request: Request,
    env: Env,
): Promise<Response> =
    GlobalScope.promise {
        val url = URL(request.url)
        val api =
            Api(D1ViewStore(env.DB)) { slug ->
                val page = env.ASSETS.fetch("${url.origin}/posts/$slug/").await()
                page.unsafeCast<FetchedResponse>().status == 200
            }
        val response = api.handle(request.method, url.pathname)

        val headers: dynamic = js("({})")
        response.headers.forEach { (name, value) -> headers[name] = value }
        val init = js("({})").unsafeCast<ResponseInit>()
        init.status = response.status
        init.headers = headers
        Response(response.body, init)
    }

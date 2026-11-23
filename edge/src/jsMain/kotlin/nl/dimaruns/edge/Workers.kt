package nl.dimaruns.edge

import kotlin.js.Promise

// Minimal declarations for the parts of the Cloudflare Workers runtime this Worker uses.
// See https://developers.cloudflare.com/workers/runtime-apis/

external class Request {
    val method: String
    val url: String
}

external class Response(
    body: String?,
    init: ResponseInit,
)

external interface ResponseInit {
    var status: Int
    var headers: dynamic
}

external class URL(
    url: String,
) {
    val origin: String
    val pathname: String
}

/** Bindings declared in wrangler.jsonc. */
external interface Env {
    @Suppress("PropertyName")
    val DB: D1Database

    @Suppress("PropertyName")
    val ASSETS: Fetcher
}

external interface D1Database {
    fun prepare(query: String): D1PreparedStatement
}

external interface D1PreparedStatement {
    fun bind(vararg values: Any?): D1PreparedStatement

    /** Value of [column] in the first row, or null when there are no rows. */
    fun first(column: String): Promise<Any?>
}

/** The static-assets binding: fetches files of the generated site without leaving the Worker. */
external interface Fetcher {
    fun fetch(url: String): Promise<Response>
}

external interface FetchedResponse {
    val status: Int
}

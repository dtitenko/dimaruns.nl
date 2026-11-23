package nl.dimaruns.edge

import kotlinx.coroutines.test.runTest
import nl.dimaruns.shared.Slug
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiTest {
    private class InMemoryViews : ViewStore {
        val counts = mutableMapOf<String, Int>()

        override suspend fun get(slug: Slug) = counts[slug.value] ?: 0

        override suspend fun increment(slug: Slug) = (get(slug) + 1).also { counts[slug.value] = it }
    }

    private val views = InMemoryViews()
    private val api = Api(views) { it.value == "hello-world" }

    @Test
    fun healthCheck() =
        runTest {
            assertEquals(
                ApiResponse(200, """{"status":"ok"}""", api.handle("GET", "/api/health").headers),
                api.handle("GET", "/api/health"),
            )
        }

    @Test
    fun countsViewsOfExistingPosts() =
        runTest {
            assertEquals("""{"slug":"hello-world","views":0}""", api.handle("GET", "/api/views/hello-world").body)
            api.handle("POST", "/api/views/hello-world")
            val response = api.handle("POST", "/api/views/hello-world")

            assertEquals(200, response.status)
            assertEquals("""{"slug":"hello-world","views":2}""", response.body)
            assertEquals("no-store", response.headers["cache-control"])
        }

    @Test
    fun doesNotCountPostsThatDoNotExist() =
        runTest {
            assertEquals(404, api.handle("POST", "/api/views/made-up").status)
            assertEquals(emptyMap(), views.counts)
        }

    @Test
    fun rejectsInvalidSlugs() =
        runTest {
            listOf("/api/views/", "/api/views/Bad", "/api/views/a/b", "/api/views/%3Cscript%3E").forEach {
                assertEquals(400, api.handle("POST", it).status, it)
            }
            assertEquals(emptyMap(), views.counts)
        }

    @Test
    fun unknownRoutesAndMethods() =
        runTest {
            assertEquals(404, api.handle("GET", "/api/nope").status)
            val response = api.handle("DELETE", "/api/views/hello-world")
            assertEquals(405, response.status)
            assertEquals("GET, POST", response.headers["allow"])
            assertEquals(405, api.handle("POST", "/api/health").status)
        }
}

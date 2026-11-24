package dimaruns.generator.site

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.SimpleFileServer
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.walk

private const val RELOAD_SCRIPT =
    "<script>let v;setInterval(()=>fetch('/__reload').then(r=>r.text())" +
        ".then(n=>{if(v&&n!==v)location.reload();v=n}).catch(()=>{}),1000)</script>"

internal fun injectReload(html: String): String = html.replace("</body>", "$RELOAD_SCRIPT</body>")

fun snapshot(dirs: List<Path>): Set<String> =
    try {
        dirs.filter { it.exists() }.flatMapTo(HashSet()) { dir ->
            dir.walk().map { "$it ${it.getLastModifiedTime()}" }
        }
    } catch (_: IOException) {
        setOf("changing ${System.nanoTime()}")
    }

class DevServer(
    private val root: Path,
    port: Int,
) {
    private val version = AtomicLong()
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0)

    init {
        val files = SimpleFileServer.createFileHandler(root)
        server.createContext("/__reload") { ex ->
            val body = version.get().toString().toByteArray()
            ex.responseHeaders.set("Cache-Control", "no-store")
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }
        server.createContext("/") { ex ->
            ex.responseHeaders.set("Cache-Control", "no-store")
            val path = ex.requestURI.path
            val file =
                root.resolve(path.removePrefix("/") + if (path.endsWith("/")) "index.html" else "").normalize()
            if (ex.requestMethod == "GET" &&
                file.startsWith(root) &&
                file.toString().endsWith(".html") &&
                file.isRegularFile()
            ) {
                val body = injectReload(file.readText()).toByteArray()
                ex.responseHeaders.set("Content-Type", "text/html; charset=utf-8")
                ex.sendResponseHeaders(200, body.size.toLong())
                ex.responseBody.use { it.write(body) }
            } else {
                files.handle(ex)
            }
        }
    }

    fun start() = server.start()

    fun reload() {
        version.incrementAndGet()
    }
}

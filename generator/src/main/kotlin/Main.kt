package dimaruns.generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.sun.net.httpserver.SimpleFileServer
import dimaruns.generator.content.ContentException
import dimaruns.generator.site.BuildOptions
import dimaruns.generator.site.SiteBuilder
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.io.path.Path
import kotlin.time.TimeSource

private class Generator : CliktCommand(name = "generator") {
    override fun help(context: Context) = "Static-site generator for dimaruns.nl"

    override fun run() = Unit
}

private class Build : CliktCommand(name = "build") {
    override fun help(context: Context) = "Render content into a static site"

    private val content by option(help = "Directory with site.yaml and posts/")
        .path(mustExist = true, canBeFile = false)
        .default(Path("content"))
    private val static by option(help = "Directory copied verbatim into the output")
        .path(canBeFile = false)
        .default(Path("static"))
    private val out by option(help = "Output directory (fully replaced)").path().default(Path("build/site"))
    private val drafts by option(help = "Include draft posts").flag()
    private val baseUrl by option(help = "Override baseUrl from site.yaml, e.g. for preview deploys")

    override fun run() {
        val started = TimeSource.Monotonic.markNow()
        val report =
            try {
                SiteBuilder().build(BuildOptions(content, static, out, drafts, baseUrl))
            } catch (e: ContentException) {
                echo(e.message, err = true)
                throw ProgramResult(1)
            }
        echo(
            "Built ${report.posts} post(s), ${report.tags} tag(s), ${report.files} file(s) " +
                "into $out in ${started.elapsedNow().inWholeMilliseconds} ms",
        )
    }
}

private class Serve : CliktCommand(name = "serve") {
    override fun help(context: Context) = "Serve a built site locally (preview only, not for production)"

    private val dir by option(help = "Directory to serve")
        .path(mustExist = true, canBeFile = false)
        .default(Path("build/site"))
    private val port by option(help = "Port to listen on").int().default(DEFAULT_PORT)

    override fun run() {
        val address = InetSocketAddress(InetAddress.getLoopbackAddress(), port)
        val server =
            SimpleFileServer.createFileServer(
                address,
                dir.toAbsolutePath().normalize(),
                SimpleFileServer.OutputLevel.INFO,
            )
        server.start()
        echo("Serving $dir on http://localhost:$port (Ctrl+C to stop)")
    }

    companion object {
        const val DEFAULT_PORT = 8080
    }
}

fun main(args: Array<String>) = Generator().subcommands(Build(), Serve()).main(args)

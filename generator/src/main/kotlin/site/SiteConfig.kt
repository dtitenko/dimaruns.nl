package dimaruns.generator.site

import com.charleskorn.kaml.Yaml
import dimaruns.generator.content.ContentException
import dimaruns.generator.content.Problem
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.readText

/** Site-wide settings from `content/site.yaml`. */
@Serializable
data class SiteConfig(
    val title: String,
    val description: String,
    val author: String,
    val baseUrl: String,
    val language: String = "en",
) {
    /** Absolute URL, needed where relative links are not allowed (feeds, sitemap, canonical links). */
    fun absolute(path: String): String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')

    fun validate(): List<String> =
        buildList {
            if (!BASE_URL.matches(baseUrl)) add("baseUrl '$baseUrl' must be an absolute http(s) URL without a path")
            if (title.isBlank()) add("title must not be blank")
            if (author.isBlank()) add("author must not be blank")
        }

    companion object {
        private val BASE_URL = Regex("^https?://[^/\\s]+/?$")

        fun load(
            file: Path,
            baseUrlOverride: String? = null,
        ): SiteConfig {
            val source = file.fileName.toString()
            val config =
                try {
                    Yaml.default.decodeFromString(serializer(), file.readText())
                } catch (e: IllegalArgumentException) {
                    throw ContentException(listOf(Problem(source, e.message ?: e.toString())))
                }.let { if (baseUrlOverride != null) it.copy(baseUrl = baseUrlOverride) else it }
            val problems = config.validate()
            if (problems.isNotEmpty()) throw ContentException(problems.map { Problem(source, it) })
            return config
        }
    }
}

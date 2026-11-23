package nl.dimaruns.generator.content

/** A single problem in a content file, e.g. invalid front-matter. */
data class Problem(
    val source: String,
    val message: String,
) {
    override fun toString(): String = "$source: $message"
}

/** Thrown when content is invalid. Carries every problem found, so authors can fix them all in one go. */
class ContentException(
    val problems: List<Problem>,
) : Exception("Found ${problems.size} content problem(s):\n" + problems.joinToString("\n") { "  - $it" })

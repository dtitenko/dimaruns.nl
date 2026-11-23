// Shared quality gates: formatting/linting (ktlint) and coverage (Kover).
plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlinx.kover")
}

ktlint {
    filter {
        exclude { it.file.path.contains("/build/") || it.file.path.contains("\build\\") }
    }
}

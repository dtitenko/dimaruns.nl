plugins {
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":shared"))
    kover(project(":generator"))
}

plugins {
    id("dimaruns.kotlin-jvm")
    application
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.bundles.commonmark)
    implementation(libs.kotlinx.html)
    implementation(libs.openhtmltopdf)
    implementation(libs.kaml)
    implementation(libs.clikt)
}

application {
    mainClass = "dimaruns.generator.MainKt"
}

val contentDir = layout.settingsDirectory.dir("content")
val staticDir = layout.settingsDirectory.dir("static")
val siteDir = layout.buildDirectory.dir("site")

// -Pdrafts=true includes draft posts; -PbaseUrl=https://... overrides site.yaml (used for preview deploys).
val includeDrafts = providers.gradleProperty("drafts").map(String::toBoolean).orElse(false)
val baseUrl = providers.gradleProperty("baseUrl").orElse("")

val generateSite =
    tasks.register<JavaExec>("generateSite") {
        group = "site"
        description = "Renders content/ and static/ into a deployable static site."

        classpath = sourceSets.main.get().runtimeClasspath
        mainClass = application.mainClass

        inputs.dir(contentDir).withPropertyName("content").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.dir(staticDir).withPropertyName("static").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.property("drafts", includeDrafts)
        inputs.property("baseUrl", baseUrl)
        outputs.dir(siteDir).withPropertyName("site")
        // Output is a pure function of the inputs above plus the generator classpath, so it is safe to cache.
        outputs.cacheIf { true }

        // Locals, not script-level vals: a lambda that references script vals captures the whole build
        // script, which the configuration cache cannot serialize.
        val content = contentDir.asFile.path
        val static = staticDir.asFile.path
        val out = siteDir
        val drafts = includeDrafts
        val url = baseUrl
        argumentProviders.add(
            CommandLineArgumentProvider {
                buildList {
                    addAll(listOf("build", "--content", content, "--static", static, "--out", out.get().asFile.path))
                    if (drafts.get()) add("--drafts")
                    url.get().takeIf { it.isNotEmpty() }?.let { addAll(listOf("--base-url", it)) }
                }
            },
        )
    }

tasks.register<JavaExec>("serveSite") {
    group = "site"
    description = "Serves the generated site on http://localhost:8080 (use -Pdrafts=true to include drafts)."
    dependsOn(generateSite)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    val out = siteDir
    argumentProviders.add(CommandLineArgumentProvider { listOf("serve", "--dir", out.get().asFile.path) })
}

tasks.register<JavaExec>("devSite") {
    group = "site"
    description = "Serves the site on http://localhost:8080, rebuilding and live-reloading on content/static changes."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    val content = contentDir.asFile.path
    val static = staticDir.asFile.path
    val out = layout.buildDirectory.dir("dev-site")
    val drafts = includeDrafts
    argumentProviders.add(
        CommandLineArgumentProvider {
            buildList {
                addAll(listOf("dev", "--content", content, "--static", static, "--out", out.get().asFile.path))
                if (drafts.get()) add("--drafts")
            }
        },
    )
}

// SiteBuilderTest checks the real icon sprite against the icons the templates reference.
tasks.test {
    val sprite = staticDir.file("assets/icons.svg")
    inputs.file(sprite).withPropertyName("iconSprite").withPathSensitivity(PathSensitivity.NONE)
    systemProperty("iconSprite", sprite.asFile.path)
}

// CI's `build` also proves that all content is valid and the site renders.
tasks.named("build") { dependsOn(generateSite) }

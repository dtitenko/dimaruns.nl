// Convention for Kotlin Multiplatform modules. Each module declares its own targets:
// `shared` builds for JVM + JS, `edge` (the Cloudflare Worker) for JS only.
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("dimaruns.quality")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
    compilerOptions {
        allWarningsAsErrors = true
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Node.js/Yarn are still downloaded, but from the repositories declared in settings.gradle.kts
// (the build forbids project-level repositories). The specs only exist once a module declares a
// JS target, so react to the Kotlin/JS plugins being applied rather than looking them up now.
listOf(project, rootProject).forEach { p ->
    p.plugins.withType<NodeJsPlugin>().configureEach { p.extensions.findByType<NodeJsEnvSpec>()?.disableRepository() }
    p.plugins.withType<NodeJsRootPlugin>().configureEach {
        p.extensions.findByType<NodeJsEnvSpec>()?.disableRepository()
    }
    p.plugins.withType<YarnPlugin>().configureEach { p.extensions.findByType<YarnRootEnvSpec>()?.disableRepository() }
}

fun EnvSpec<*>.disableRepository() = downloadBaseUrl.set(null as String?)

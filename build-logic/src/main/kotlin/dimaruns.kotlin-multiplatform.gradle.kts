// Convention for code shared between the JVM generator and the JS edge Worker.
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
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

    jvm()
    js {
        nodejs()
        useEsModules()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Node.js/Yarn are still downloaded, but from the repositories declared in settings.gradle.kts
// (the build forbids project-level repositories).
listOf(project, rootProject).forEach { p ->
    p.extensions.findByType<NodeJsEnvSpec>()?.disableRepository()
    p.extensions.findByType<YarnRootEnvSpec>()?.disableRepository()
}

fun EnvSpec<*>.disableRepository() = downloadBaseUrl.set(null as String?)

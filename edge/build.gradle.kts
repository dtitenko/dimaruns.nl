plugins {
    id("dimaruns.kotlin-multiplatform")
}

kotlin {
    js {
        outputModuleName = "dimaruns-edge"
        // Workers run on V8, not Node, but the Node target gives a plain ES module without a
        // browser bundle; wrangler bundles it with worker.mjs at deploy time.
        nodejs()
        useEsModules()
        binaries.library()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.kotlinx.coroutines.core)
        }
        jsTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Copies the optimised (dead-code-eliminated) JS into build/worker, where worker.mjs imports it.
val assembleWorker =
    tasks.register<Sync>("assembleWorker") {
        group = "site"
        description = "Builds the production Worker module into build/worker."
        from(tasks.named("jsNodeProductionLibraryDistribution"))
        into(layout.buildDirectory.dir("worker"))
    }

tasks.named("assemble") { dependsOn(assembleWorker) }

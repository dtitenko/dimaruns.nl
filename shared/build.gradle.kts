plugins {
    id("dimaruns.kotlin-multiplatform")
}

kotlin {
    jvm()
    js {
        nodejs()
        useEsModules()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.core)
            api(libs.kotlinx.datetime)
        }
    }
}

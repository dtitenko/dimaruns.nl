plugins {
    id("dimaruns.kotlin-jvm")
    application
}

dependencies {
    implementation(project(":shared"))
}

application {
    mainClass = "nl.dimaruns.generator.MainKt"
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}

plugins {
    application
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.execfork)
}

application {
    mainClass.set("dev.fritz2.ServerKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.calllogging)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.contentnegotiation)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.plugin.jacksonserialization)
    implementation(libs.logback.classic)
}

tasks.register<com.github.psxpaul.task.JavaExecFork>("start") {
    classpath = sourceSets.main.map { it.runtimeClasspath }.get()
    main = application.mainClass.get()
    workingDir = layout.buildDirectory.asFile.get()
    standardOutput = "$workingDir/server.log"
    errorOutput = "$workingDir/error.log"
    stopAfter = project(":core").tasks["check"]
    waitForPort = 3000
}
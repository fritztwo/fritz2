plugins {
    `kotlin-dsl`
}

// Apply the actual plugin to the project/module using our publishing plugin
pluginManager.withPlugin(libs.vanniktech.mavenpublishplugin.get().name) {
    version = libs.vanniktech.mavenpublishplugin.get().version!!
}

/*
This is one of the _two_ places the JVM version of the project is configured.

All multiplatform modules of this project targeting the JVM apply the `fritz2-jvm-conventions` plugin which configures a
common toolchain version to use (single point of configuration). The other place is the configuration block below, which
configures the JVM toolchain for the `buildSrc` module itself.
 */
kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.vanniktech.mavenpublishplugin)
}
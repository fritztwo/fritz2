plugins {
    `kotlin-dsl`
}

// Apply the actual plugin to the project/module using our publishing plugin
pluginManager.withPlugin(libs.vanniktech.mavenpublishplugin.get().name) {
    version = libs.vanniktech.mavenpublishplugin.get().version!!
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.vanniktech.mavenpublishplugin)
}
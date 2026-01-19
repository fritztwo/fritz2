plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.dokka)
    id("fritz2-publishing-conventions")
}

kotlin {
    jvm()
    js(IR).browser { }
    sourceSets {
        jsMain {
            dependencies {
                api(project(":core"))
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}
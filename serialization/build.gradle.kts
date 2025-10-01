plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    id("fritz2-publishing-config")
}

kotlin {
    jvm()
    js(IR).browser { }
    sourceSets {
        jsMain {
            dependencies {
                api(project(":core"))
                api(KotlinX.serialization.json)
            }
        }
    }
}
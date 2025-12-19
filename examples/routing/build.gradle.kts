plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser()
    }.binaries.executable()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
            }
        }
        jsMain {
            dependencies {
            }
        }
    }
}
import dev.fritz2.gradle.npm

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
}

kotlin {
    js(IR) {
        browser()
    }.binaries.executable()

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.ExperimentalStdlibApi")
            }
        }
        jsMain {
            dependencies {
                implementation(project(":core"))
                
                // tailwind
                implementation(npm(libs.tailwindcss.core))
                implementation(npm(libs.tailwindcss.forms))

                // webpack
                implementation(npm(libs.postcss.core))
                implementation(npm(libs.postcss.loader))
                implementation(npm(libs.autoprefixer))
                implementation(npm(libs.css.loader))
                implementation(npm(libs.style.loader))
                implementation(npm(libs.cssnano))
            }
        }
    }
}


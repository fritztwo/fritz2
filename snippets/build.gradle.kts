import dev.fritz2.gradle.npm

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
            }
        }
        binaries.executable()
    }

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
                implementation(npm(libs.tailwindcss.postcss))
                implementation(npm(libs.postcss.core))
                implementation(npm(libs.postcss.loader))
            }
        }
    }
}


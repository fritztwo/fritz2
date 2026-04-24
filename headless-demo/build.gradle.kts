import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import dev.fritz2.gradle.npm

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.google.ksp)
    id("fritz2-jvm-conventions")
}

kotlin {
    jvm() // needed for kspCommonMainMetadata

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
        commonMain {
            dependencies {
                implementation(project(":headless"))
            }
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
        jsMain {
            dependencies {
                implementation(npm(libs.tailwindcss.core))
                implementation(npm(libs.tailwindcss.postcss))
                implementation(npm(libs.postcss.core))
                implementation(npm(libs.postcss.loader))
            }
        }
    }
}

dependencies {
    kspCommonMainMetadata(project(":lenses-annotation-processor"))
}

project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

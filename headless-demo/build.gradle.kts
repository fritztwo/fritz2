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
                    enabled = true // Aktiviert die Verarbeitung von CSS-Dateien
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
                // Das Core-Paket und das PostCSS-Plugin müssen immer die exakt gleiche Version haben
                implementation(npm("tailwindcss", "4.2.0"))
                implementation(npm("@tailwindcss/postcss", "4.2.0"))

                // PostCSS selbst (Version 8.4.x ist der stabile Standard für Tailwind 4)
                implementation(npm("postcss", "8.4.47"))

                // Der Loader für Webpack. Version 7.3.3 ist sehr stabil für Kotlin/JS.
                // Höhere Versionen (z.B. 8.x oder 9.x) könnten neuere Node-Versionen
                // voraussetzen, die dein Kotlin-Plugin eventuell noch nicht mitbringt.
                implementation(npm("postcss-loader", "7.3.3"))
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

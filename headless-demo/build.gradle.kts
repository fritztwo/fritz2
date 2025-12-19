import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import dev.fritz2.gradle.npm

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.google.ksp)
}

kotlin {
    jvm() // needed for kspCommonMainMetadata

    js(IR) {
        browser()
    }.binaries.executable()

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

dependencies {
    kspCommonMainMetadata(project(":lenses-annotation-processor"))
}

project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
    if(name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

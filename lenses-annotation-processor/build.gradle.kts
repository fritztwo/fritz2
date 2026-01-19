plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.jetbrains.dokka)
    id("fritz2-publishing-conventions")
}

ksp {
    arg("autoserviceKsp.verify", "true")
    arg("autoserviceKsp.verbose", "true")
}

kotlin {
    js(IR) {
        browser()
    }.binaries.executable()
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets {
        jvmMain {
            dependencies {
                api(project(":core"))
                api(libs.kotlin.stdlib)
                api(libs.kotlin.reflect)
                api(libs.kotlin.scriptruntime)
                implementation(libs.kotlinpoet)
                implementation(libs.kotlinpoet.ksp)
                implementation(libs.google.autoservice.annotations)
                implementation(libs.google.ksp.symbolprocessing)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotlin.test.junit5)
                implementation(libs.junit.jupiter.params)
                implementation(libs.assertj.core)
                implementation(libs.zacsweers.kctfork.core)
                implementation(libs.zacsweers.kctfork.ksp)
            }
        }
    }
}

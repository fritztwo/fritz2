plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.dokka)
    id("fritz2-publishing-config")
}

kotlin {
    jvm()
    js(IR).browser {
        testTask {
            //running test-server in background
            dependsOn(":test-server:start")
            // see "karma.config.d" folder for customizing karma
        }
    }
    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.coroutines.FlowPreview")
            }
        }
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.common)
                implementation(libs.kotlin.test.annotations.common)
            }
        }
        jsMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
            }
        }
        jsTest {
            dependencies {
                implementation(libs.kotlin.test.js)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

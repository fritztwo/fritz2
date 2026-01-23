import dev.fritz2.gradle.npm

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.jetbrains.dokka)
    id("fritz2-publishing-conventions")
}

kotlin {
    jvm()
    js(IR).browser {
        testTask {
            useKarma {
//                useSafari()
//                useFirefox()
//                useChrome()
                useChromeHeadless()
//                usePhantomJS()
            }
        }
    }
    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        commonMain {
            dependencies {
                api(project(":core"))
            }
        }
        jsMain {
            dependencies {
                api(npm(libs.floatingui.dom))
                api(npm(libs.scrollintoview))
            }
        }
        jsTest {
            dependencies {
                implementation(libs.kotlin.test.js)
            }
        }
    }
}

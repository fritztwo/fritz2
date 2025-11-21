import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
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
                api(KotlinX.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(Kotlin.test)
                implementation(Kotlin.test.common)
                implementation(Kotlin.test.annotationsCommon)
            }
        }
        jsMain {
            dependencies {
                api(KotlinX.coroutines.core)
            }
        }
        jsTest {
            dependencies {
                implementation(Kotlin.test.js)
                implementation(KotlinX.serialization.json)
            }
        }
    }
}

// apply(from = "$rootDir/publishing.gradle.kts")

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    // sources publishing is always enabled by the Kotlin Multiplatform plugin
    configure(KotlinMultiplatform(
        // configures the -javadoc artifact, possible values:
        // - `JavadocJar.None()` don't publish this artifact
        // - `JavadocJar.Empty()` publish an empty jar
        // - `JavadocJar.Dokka("dokkaHtml")` when using Kotlin with Dokka, where `dokkaHtml` is the name of the Dokka task that should be used as input
        javadocJar = JavadocJar.Dokka("dokkaHtml"),
        // whether to publish a sources jar
        sourcesJar = true,
    ))
}

mavenPublishing {
    coordinates("dev.fritz2", "core", "1.0-SNAPSHOT")

    pom {
        name.set("fritz2")
        description.set("Easily build reactive web-apps in Kotlin based on flows and coroutines")
        inceptionYear.set("2020")
        url.set("https://www.fritz2.dev/")
        /*licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }*/
        developers {
            developer {
                id.set("lysander")
                name.set("Christian Hausknecht")
                email.set("christian.hausknecht@gmx.de")
            }
            developer {
                id.set("haukesomm")
                name.set("Hauke Sommerfeld")
                email.set("development@haukesomm.de")
            }
            developer {
                id.set("loesking")
                name.set("Lukas Lösking")
                email.set("sonatype@loesking.com")
            }
            developer {
                id.set("zqirui")
                name.set("Qirui Zhu ")
                email.set("qrno98@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/fritztwo/fritz2")
            connection.set("scm:git:git://github.com/fritztwo/fritz2.git")
            developerConnection.set("scm:git:ssh://git@github.com/fritztwo/fritz2.git")
        }
    }
}

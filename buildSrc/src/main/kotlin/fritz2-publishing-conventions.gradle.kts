import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishBasePlugin
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.kotlin.dsl.configure

/*
This plugin is available in any build script of the fritz2 project and can be applied in order to automatically
configure Maven central publishing for the module it is applied to.

When applied, nothing else needs to be configured manually - this plugin takes care of everything, including Maven
coordinates, POM and other metadata configuration.

Usage:
```kotlin
plugins {
    id("fritz2-publishing-conventions")
}
```
 */

apply<MavenPublishBasePlugin>()

extensions.configure<MavenPublishBaseExtension> {
    publishToMavenCentral()
    signAllPublications()

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"),
            sourcesJar = SourcesJar.Sources(),
        )
    )

    coordinates(project.group.toString(), project.name, project.version.toString())

    // Currently, there is an issue with the publishing plugin, preventing us from publishing to the local Maven
    // repository for testing (see: https://github.com/vanniktech/gradle-maven-publish-plugin/issues/1113).
    // This is a variant of the workaround described in the link above.
    project.gradle.taskGraph.whenReady {
        val taskIsMavenLocal = allTasks.any {
            it.name == "publishToMavenLocal" || it.path.endsWith("publishToMavenLocal")
        }
        if (taskIsMavenLocal) {
            project.extensions.getByType<SigningExtension>().isRequired = false
        }
    }

    pom {
        name.set("fritz2")
        description.set("Easily build reactive web-apps in Kotlin based on flows and coroutines")
        inceptionYear.set("2020")
        url.set("https://www.fritz2.dev/")
        
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        
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
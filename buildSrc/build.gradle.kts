import de.fayard.refreshVersions.core.versionFor

// Get the version of our third-party publishing plugin via refreshVersions
val mavenPublishPluginVersion = versionFor("com.vanniktech:gradle-maven-publish-plugin:_")

plugins {
    `kotlin-dsl`
}

// Apply the actual plugin to the project/module using our publishing plugin
pluginManager.withPlugin("com.vanniktech.maven.publish") {
    version = mavenPublishPluginVersion
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${versionFor(Kotlin.gradlePlugin)}")
    implementation("com.vanniktech:gradle-maven-publish-plugin:$mavenPublishPluginVersion")
}
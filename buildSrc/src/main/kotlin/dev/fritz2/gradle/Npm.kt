package dev.fritz2.gradle

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

// FIXME: Simple workaround to make version catalogs usable for npm dependencies too. Remove if kotlin plugin
//  supports this out of the box!
fun KotlinDependencyHandler.npm(dependency: Provider<MinimalExternalModuleDependency>): Dependency =
    dependency.map { dep ->
        val name = if (dep.group == "npm") dep.name else "@${dep.group}/${dep.name}"
        npm(name, dep.version!!)
    }.get()
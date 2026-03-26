import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/*
Configures a common JVM toolchain version for all modules using this conventions plugin.
 */
extensions.configure<KotlinMultiplatformExtension> {
    jvmToolchain(21)
}
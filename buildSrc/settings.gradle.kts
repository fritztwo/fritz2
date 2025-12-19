// Version catalogs in buildSrc need to be included manually as documented here:
// https://docs.gradle.org/current/userguide/version_catalogs.html#sec:buildsrc-version-catalog
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
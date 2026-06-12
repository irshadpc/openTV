// Top-level settings for the Open TV World Cup project.
// Declares plugin and dependency repositories used by every module.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Fail the build if any module tries to declare its own repositories,
    // keeping dependency resolution centralized and reproducible.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OpenTVWorldCup"
include(":app")

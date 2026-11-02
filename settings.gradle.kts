// Dateipfad: settings.gradle.kts
// Legt fest, woher Gradle Plugins und Bibliotheken (Google, MavenCentral) herunterlädt.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DienstplanApp"
include(":app")

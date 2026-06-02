pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "YTMusicBox"
include(":android:app")
include(":android:presentation")
include(":android:platform")
include(":shared:log")
include(":shared:domain")
include(":shared:data")
include(":shared:file-share")
include(":desktop")

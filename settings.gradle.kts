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
include(":app")
include(":presentation")
include(":platform")
include(":log:log-api")
include(":log:log-impl")
include(":shared:domain")
include(":shared:data")
include(":shared:file-share")
include(":desktop")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
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
include(":platform-runtime")
include(":notification")
include(":domain")
include(":data:network")
include(":data:localDatabase")
include(":data:pythonBridge")
include(":data:localFile")
include(":data:dataApi")

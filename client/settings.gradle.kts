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
    }
}

rootProject.name = "WhatsAppClone"

include(":app")
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:ui")
include(":feature:auth")
include(":feature:chat")
include(":feature:contacts")
include(":feature:group")
include(":feature:profile")
include(":feature:media")
include(":feature:settings")

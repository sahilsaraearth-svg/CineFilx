pluginManagement {
    repositories {
        maven { url = uri("file:///home/user/local-maven") }
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
        maven { url = uri("file:///home/user/local-maven") }
        google()
        mavenCentral()
    }
}

rootProject.name = "CineFilx"
include(":app")

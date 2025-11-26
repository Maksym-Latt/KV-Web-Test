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
        maven("https://jitpack.io")
        maven("https://maven.google.com")
    }
    plugins {
        id("com.google.dagger.hilt.android") version "2.50"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://maven.google.com")
    }
}

rootProject.name = "Web Test"
include(":app")
 
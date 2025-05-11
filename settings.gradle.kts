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
        maven { url = uri("https://jitpack.io") } // Add JitPack for MPAndroidChart
    }
    plugins {
        kotlin("jvm") version "2.0.20"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Enforce settings-level repositories
    repositories {
        google() // All Android dependencies
        mavenCentral() // General dependencies
        maven { url = uri("https://jitpack.io") } // External libraries like MPAndroidChart
    }
}

rootProject.name = "SmartRideDBD"
include(":app")
project(":app").projectDir = file("app")

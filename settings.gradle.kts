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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Win10 Start"

include(":app")

// Shared Windows Metro design system (MetroSuite/design), consumed as a composite build.
includeBuild("../../design")
// Cross-APK live tile protocol (MetroSuite/shared/live-tile-contract).
includeBuild("../../shared/live-tile-contract")

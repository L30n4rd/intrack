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

rootProject.name = "intrack"
include(":app")


// To generate libs used in this sample:
//   1) enable the gen-libs at end of this file
//   2) enable module build dependency in app/build.gradle
//   3) build the app's APK in Android Studio or on command line
//   4) undo step 1) and 2) above
include(":gen-libs")

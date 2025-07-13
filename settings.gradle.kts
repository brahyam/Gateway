rootProject.name = "gateway-kmp"
includeBuild("build-support")

include(":openai-core")
include(":openai-client")
include(":openai-client-bom")

include(":gateway-client")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}
include(":sample:android")
include(":sample:kmp:composeApp")

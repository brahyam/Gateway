plugins {
    alias(libs.plugins.kotlin.multiplaform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kotlinx.binary.validator)
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
    id("build-support")
}

kotlin {
    explicitApi()
    jvm()
    native()

    sourceSets {
        all {
            languageSettings.apply{
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.io.core)
                api(libs.serialization.json)
                implementation(libs.serialization.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

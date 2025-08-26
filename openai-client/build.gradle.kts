import org.jetbrains.kotlin.konan.target.HostManager

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
            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("okio.ExperimentalFileSystem")
                optIn("com.aallam.openai.api.ExperimentalOpenAI")
                optIn("com.aallam.openai.api.BetaOpenAI")
                optIn("com.aallam.openai.api.InternalOpenAI")
                optIn("com.aallam.openai.api.LegacyOpenAI")
            }
        }
        val commonMain by getting {
            dependencies {
                api(projects.openaiCore)
                api(libs.coroutines.core)
                api(libs.kotlinx.io.core)
                implementation(libs.kotlinx.io.bytestring)
                implementation(libs.serialization.json)
                api(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.openaiCore)
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.coroutines.test)
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libs.ktor.client.okhttp)
                implementation(libs.logback.classic)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
        if (HostManager.hostIsMac) {
            val darwinTest by getting {
                dependencies {
                    implementation(libs.ktor.client.darwin)
                }
            }
        }
    }
}

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import java.io.FileInputStream
import java.util.Properties

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.vanniktech.maven.publish")
    id("binary-compatibility-validator")
    id("com.diffplug.spotless")
    id("org.jetbrains.dokka")
    id("build-support")
    id("com.github.gmazzo.buildconfig")
}

buildConfig {
    val localProperties = Properties().apply {
        load(FileInputStream(File(rootProject.rootDir, "local.properties")))
    }
    val gradleProperties = Properties().apply {
        load(FileInputStream(File(rootProject.rootDir, "gradle.properties")))
    }
    buildConfigField("GATEWAY_VERSION", gradleProperties.getProperty("VERSION_NAME"))
    buildConfigField("GATEWAY_HOST", localProperties.getProperty("GATEWAY_HOST"))
    buildConfigField(
        "GATEWAY_PINS",
        arrayOf(
            localProperties.getProperty("GATEWAY_PIN_1"),
            localProperties.getProperty("GATEWAY_PIN_2"),
            localProperties.getProperty("GATEWAY_PIN_3")
        )
    )
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    namespace = "io.github.brahyam.gateway.client"
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    explicitApi()
    jvmToolchain(11)
    androidTarget()

    // XCFramework configuration for Swift Package Manager
    val xcframeworkName = "Gateway"
    val xcf = XCFramework(xcframeworkName)
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = xcframeworkName
            // Specify CFBundleIdentifier to uniquely identify the framework
            binaryOption("bundleId", "io.github.brahyam.gateway.${xcframeworkName}")
            xcf.add(this)
            isStatic = true
        }
    }

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
                api(projects.openaiClient)
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
        androidMain.dependencies {
            implementation(libs.play.integrity)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

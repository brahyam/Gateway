import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.brahyam.gateway.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        val prop = Properties().apply {
            load(FileInputStream(File(rootProject.rootDir, "local.properties")))
        }
        applicationId = "io.github.brahyam.gateway.sample"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add your Google Cloud Project Number and OpenAI API Key to local.properties file
        buildConfigField(
            "Long",
            "GOOGLE_CLOUD_PROJECT_NUMBER",
            prop.getProperty("GOOGLE_CLOUD_PROJECT_NUMBER")
        )
        buildConfigField("String", "OPENAI_API_KEY", "\"${prop.getProperty("OPENAI_API_KEY")}\"")
        buildConfigField(
            "String",
            "GATEWAY_PARTIAL_KEY",
            "\"${prop.getProperty("GATEWAY_PARTIAL_KEY")}\""
        )
        buildConfigField(
            "String",
            "GATEWAY_SERVICE_URL",
            "\"${prop.getProperty("GATEWAY_SERVICE_URL")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Gateway Client
    implementation(projects.gatewayClient)
    // Gateway ktor client
    implementation(libs.ktor.client.okhttp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.multiplaform) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.kotlinx.binary.validator) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.dokka)
    `maven-publish`
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "maven-publish")

    publishing {
        repositories {
            maven {
                name = "githubPackages"
                url = uri("https://maven.pkg.github.com/brahyam/gateway-kmp")
                // username and password (a personal Github access token) should be specified as
                // `githubPackagesUsername` and `githubPackagesPassword` Gradle properties or alternatively
                // as `ORG_GRADLE_PROJECT_githubPackagesUsername` and `ORG_GRADLE_PROJECT_githubPackagesPassword`
                // environment variables
                credentials(PasswordCredentials::class)
            }
        }
    }

    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    tasks.withType<Test> {
        testLogging {
            events(STARTED, PASSED, SKIPPED, FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }

    tasks.withType<KotlinJvmTest>().configureEach {
        environment("LIB_ROOT", rootDir)
    }

    tasks.withType<KotlinNativeTest>().configureEach {
        environment("SIMCTL_CHILD_LIB_ROOT", rootDir)
        environment("LIB_ROOT", rootDir)
    }

    tasks.withType<KotlinJsTest>().configureEach {
        environment("LIB_ROOT", rootDir.toString())
    }
}

tasks.withType<DokkaMultiModuleTask>() {
    outputDirectory.set(projectDir.resolve("docs"))
}

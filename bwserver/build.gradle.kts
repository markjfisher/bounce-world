import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    // This gives a "java not compatible with multiplatform" warning, but the web app doesn't work without it
    alias(libs.plugins.kvision)
    alias(libs.plugins.taskinfo)
}

group = "space-apps.bwserver"
version = "2.0.0"

//application {
//    mainClass.set("bw.AppKt")
//
//    val isDevelopment: Boolean = project.ext.has("development")
//    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
//}

kotlin {
    jvmToolchain(17)
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "main.bundle.js"
                sourceMaps = true
            }
        }
        binaries.executable()
    }
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            // THIS IS NOT GETTING THROUGH TO THE KTOR INSTANCE - todo: fix it
            // use -PisDevelopment when building with gradle for this
            val isDevelopment: Boolean = project.hasProperty("isDevelopment")
            listOf("-Dio.ktor.development=$isDevelopment")
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("bw.AppKt")
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kvision.server.ktor)
            }
        }
        jvmMain {
            dependencies {
                implementation(project(":core"))
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.server.compression)
                // This stops the hocon loader from being put in the the jar file at META-INF/services/io.ktor.server.config.ConfigLoader
                // implementation(libs.ktor.server.config.yaml)
                implementation(libs.logback.classic)
                implementation(libs.joml.core)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotest.ktor.core)
                implementation(libs.kotest.ktor.assertions)
                implementation(libs.ktor.server.test.host)
                implementation(libs.kotest.runner.junit5)
                implementation(libs.mockk.core)
            }

        }
        jsMain {
            dependencies {
                implementation(libs.kvision.core)
                implementation(libs.kvision.bootstrap)
                implementation(libs.kvision.state)
                implementation(libs.kvision.fontawesome)
                // implementation(libs.kvision.i18n)
            }

        }
    }
}
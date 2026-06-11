import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kilua.rpc)
    alias(libs.plugins.kvision)
    alias(libs.plugins.taskinfo)
}

group = "bounce.world"
version = "2.2.1"

kotlin {
    jvmToolchain(25)
    js {
        browser {
            useEsModules()
            commonWebpackConfig {
                outputFileName = "main.bundle.js"
                sourceMaps = true
            }
        }
        binaries.executable()
        compilerOptions {
            target.set("es2015")
        }
    }
    jvm {
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
                implementation(libs.kilua.rpc.ktor)
                implementation(libs.kvision.common.remote)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.server.compression)
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
            }
        }
    }
}

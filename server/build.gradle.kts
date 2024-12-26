plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    id("io.ktor.plugin") version "3.0.2"
}

version = "2.0.0"
group = "bounce.world"

val logbackVersion = "1.5.9"
val jomlVersion: String by project

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server core
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    
    // Ktor features
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    
    // Ktor client (if needed)
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    
    // Ktor websockets (if needed)
    implementation("io.ktor:ktor-server-websockets")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    
    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Keep your existing dependencies
    implementation("org.joml:joml:$jomlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.8.1")
    implementation("io.reactivex.rxjava2:rxjava")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.24")
}

application {
    mainClass = "ApplicationKtorKt"
}

java {
//    sourceCompatibility = JavaVersion.toVersion("17")
}

kotlin {
    jvmToolchain(17)
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}


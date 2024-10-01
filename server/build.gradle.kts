plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("io.ktor.plugin") version "2.3.8"
}

version = "2.0.0"
group = "bounce.world"

val ktorVersion = "2.3.8"
val logbackVersion = "1.4.14"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server core
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    
    // Ktor features
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    
    // Ktor client (if needed)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    
    // Ktor websockets (if needed)
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    
    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Keep your existing dependencies
    implementation("org.joml:joml:$jomlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.8.1")
    implementation("io.reactivex.rxjava2:rxjava")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.23")
}

application {
    mainClass = "ApplicationKt"
}

java {
//    sourceCompatibility = JavaVersion.toVersion("17")
}

allOpen {
}

kotlin {
    jvmToolchain(17)
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}


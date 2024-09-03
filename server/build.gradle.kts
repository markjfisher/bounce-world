plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.allopen") version "1.9.23"
    id("com.google.devtools.ksp") version "1.9.23-1.0.19"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.application") version "4.4.0"
    id("io.micronaut.aot") version "4.4.0"
}

version = "1.0.6"
group = "bounce.world"

val jomlVersion: String by project
val tornadoFXVersion: String by project

val assertJVersion: String by project
val mockkVersion: String by project

val kotlinVersion= project.properties["kotlinVersion"]
repositories {
    mavenCentral()
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("io.micronaut.toml:micronaut-toml")

//    implementation("no.tornado:tornadofx:$tornadoFXVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.8.1")
    implementation("io.reactivex.rxjava2:rxjava")

    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")

    compileOnly("io.micronaut:micronaut-http-client")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.slf4j:jul-to-slf4j:2.0.13")
    runtimeOnly("org.slf4j:slf4j-simple")

    implementation("org.joml:joml:$jomlVersion")

    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
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

graalvmNative.toolchainDetection = false
micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
    }
    aot {
    // Please review carefully the optimizations enabled below
    // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        // replaceLogbackXml = true
    }
}




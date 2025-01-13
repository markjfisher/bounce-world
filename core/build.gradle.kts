plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "space-apps.core"
version = "1.0.0"

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)
    implementation(libs.joml.core)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk.core)
}

tasks.test {
    useJUnitPlatform()
}
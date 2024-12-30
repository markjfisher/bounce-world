
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
}

group = "bounce.world"
version = "2.0.0"

application {
    mainClass.set("bw.AppKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.joml.core)

    testImplementation(libs.kotest.ktor.core)
    testImplementation(libs.kotest.ktor.assertions)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

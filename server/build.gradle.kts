
plugins {
    id("java")
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

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
//    testImplementation(libs.assertj.core)
    testImplementation(libs.kotest.ktor.core)
    testImplementation(libs.mockk.core)
}

//test {
//    useJUnitPlatform()
//}

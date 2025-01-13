plugins {
    alias(libs.plugins.kotlin.jvm) apply(false)
    alias(libs.plugins.kotlin.multiplatform) apply(false)
    alias(libs.plugins.kotlin.serialization) apply(false)
}

tasks {
    getByName<Wrapper>("wrapper") {
        gradleVersion = "8.12"
        distributionType = Wrapper.DistributionType.ALL
    }
}

defaultTasks(
    ":core:clean", ":core:build",
    ":bwserver:clean", ":bwserver:jar",
)

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven(url = "https://raw.githubusercontent.com/kotlin-graphics/mary/master")
        maven(url = "https://jitpack.io")
    }

}

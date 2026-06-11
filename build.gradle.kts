plugins {
    alias(libs.plugins.version.catalog.update)
}

versionCatalogUpdate {
    pin {
        versions.set(
            listOf(
                "kotlin-version",
                "ksp-version",
                "kilua-rpc-version",
                "kvision-version",
            ),
        )
    }
}

tasks {
    getByName<Wrapper>("wrapper") {
        gradleVersion = "9.1.0"
        distributionType = Wrapper.DistributionType.ALL
    }
}

defaultTasks(
    ":server:clean", ":server:jarWithJs",
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

tasks {
    getByName<Wrapper>("wrapper") {
        gradleVersion = "8.12"
        distributionType = Wrapper.DistributionType.ALL
    }
}

defaultTasks(
    // ":server:clean", ":server:jar"
    ":server:jar"
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

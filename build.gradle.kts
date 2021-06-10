import java.time.Duration

plugins {
    kotlin("jvm") version Versions.kotlin apply false
    id("io.github.gradle-nexus.publish-plugin") version Versions.`publish-plugin`
    jacoco
    base
}

allprojects {
    group = "org.taymyr.lagom"
    version = "0.14.0-SNAPSHOT"
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply<JacocoPlugin>()

    jacoco {
        toolVersion = Versions.jacoco
    }
}

nexusPublishing {
    packageGroup.set("org.taymyr")
    clientTimeout.set(Duration.ofMinutes(60))
    repositories {
        sonatype()
    }
}

val jacocoAggregateMerge by tasks.creating(JacocoMerge::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    executionData(
        project(":lagom-extensions-java").buildDir.absolutePath + "/jacoco/test.exec"
    )
    dependsOn(
        ":lagom-extensions-java:test"
    )
}

@Suppress("UnstableApiUsage")
val jacocoAggregateReport by tasks.creating(JacocoReport::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    executionData(jacocoAggregateMerge.destinationFile)
    reports {
        xml.isEnabled = true
    }
    additionalClassDirs(files(subprojects.flatMap { project ->
        listOf("scala", "kotlin").map { project.buildDir.path + "/classes/$it/main" }
    }))
    additionalSourceDirs(files(subprojects.flatMap { project ->
        listOf("scala", "kotlin").map { project.file("src/main/$it").absolutePath }
    }))
    dependsOn(jacocoAggregateMerge)
}

tasks.check { finalizedBy(jacocoAggregateReport) }

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

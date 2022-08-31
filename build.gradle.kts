import fr.brouillard.oss.jgitver.Strategies.MAVEN
import java.time.Duration

plugins {
    kotlin("jvm") version Versions.kotlin apply false
    id("io.github.gradle-nexus.publish-plugin") version Versions.`publish-plugin`
    id("fr.brouillard.oss.gradle.jgitver") version Versions.jgitver
    jacoco
    base
}

allprojects {
    group = "org.taymyr.lagom"
    repositories {
        mavenCentral()
    }
    apply<JacocoPlugin>()
    jacoco {
        toolVersion = Versions.jacoco
    }
}

jgitver {
    strategy(MAVEN)
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

val jacocoAggregateReport by tasks.creating(JacocoReport::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    executionData(jacocoAggregateMerge.destinationFile)
    reports {
        xml.required.set(true)
    }
    additionalClassDirs(files(subprojects.flatMap { project ->
        listOf("scala", "kotlin", "kotlin-2.13").map { project.buildDir.path + "/classes/$it/main" }
    }))
    additionalSourceDirs(files(subprojects.flatMap { project ->
        listOf("scala", "kotlin", "kotlin-2.13").map { project.file("src/main/$it").absolutePath }
    }))
    dependsOn(jacocoAggregateMerge)
}

tasks.check { finalizedBy(jacocoAggregateReport) }

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

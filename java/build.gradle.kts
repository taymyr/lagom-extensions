@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version Versions.dokka
    id("org.jlleitschuh.gradle.ktlint") version Versions.`ktlint-plugin`
    `maven-publish`
    signing
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"
compileKotlin.kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = "1.8"
compileTestKotlin.kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    when (scalaBinaryVersion) {
        "2.12" -> {
            implementation("org.scala-lang:scala-library") {
                version {
                    // Workaround of https://youtrack.jetbrains.com/issue/KT-38325#focus=Comments-27-4492387.0-0
                    strictly("2.12.10")
                }
            }
        }
    }
    implementation("com.typesafe.akka", "akka-stream-kafka_$scalaBinaryVersion", Versions.akkaStreamKafka)
    compileOnly("com.lightbend.lagom", "lagom-javadsl-api_$scalaBinaryVersion", lagomVersion)
    compileOnly("com.lightbend.lagom", "lagom-javadsl-server_$scalaBinaryVersion", lagomVersion)
    compileOnly("com.lightbend.lagom", "lagom-javadsl-kafka-client_$scalaBinaryVersion", lagomVersion)
    compileOnly("com.lightbend.lagom", "lagom-cluster-core_$scalaBinaryVersion", lagomVersion)
    compileOnly("org.pac4j", "lagom-pac4j_$scalaBinaryVersion", Versions.lagomPac4j)
    compileOnly("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", Versions.`kotlinx-coroutines`)
    api("io.github.microutils", "kotlin-logging", Versions.`kotlin-logging`)
    implementation(project(":lagom-extensions-core"))

    testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.junit5)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", Versions.junit5)
    testImplementation("org.junit.jupiter", "junit-jupiter-engine", Versions.junit5)
    testImplementation("org.assertj", "assertj-core", Versions.assertj)
    testImplementation("com.salesforce.kafka.test", "kafka-junit5", Versions.`kafka-junit5`)
    testImplementation("org.apache.kafka", "kafka_$scalaBinaryVersion", Versions.kafka)
    testImplementation("com.lightbend.lagom", "lagom-javadsl-testkit_$scalaBinaryVersion", lagomVersion)
    testImplementation("com.lightbend.lagom", "lagom-javadsl-integration-client_$scalaBinaryVersion", lagomVersion)
    testImplementation("com.typesafe.play", "play-akka-http-server_$scalaBinaryVersion", playVersion)
}

sourceSets.main {
    java.srcDirs("src/main/kotlin", "src/main/kotlin-$scalaBinaryVersion")
}

configurations {
    testCompile.get().extendsFrom(compileOnly.get())
}

ktlint {
    version.set(Versions.ktlint)
    outputToConsole.set(true)
    reporters.set(setOf(ReporterType.CHECKSTYLE))
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}

tasks.dokka {
    outputFormat = "javadoc"
    outputDirectory = "$buildDir/javadoc"
    configuration {
        jdkVersion = 8
        reportUndocumented = false
        externalDocumentationLink {
            url = URL("https://www.lagomframework.com/documentation/1.6.x/java/api/")
        }
        externalDocumentationLink {
            url = URL("https://static.javadoc.io/com.google.guava/guava/26.0-jre/")
        }
    }
    impliedPlatforms = mutableListOf("JVM")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "${project.name}_$scalaBinaryVersion"
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJar)
            pom(Publishing.pom)
        }
    }
}

signing {
    isRequired = isRelease
    sign(publishing.publications["maven"])
}

@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.net.URL

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka") version Versions.dokka
    id("org.jlleitschuh.gradle.ktlint") version Versions.`ktlint-plugin`
    `maven-publish`
    signing
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"
compileKotlin.kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = "1.8"
compileTestKotlin.kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"

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
    compileOnly("org.jetbrains.kotlinx", "kotlinx-serialization-json", Versions.`kotlinx-serialization`)
    compileOnly("com.typesafe.play", "play-cache_$scalaBinaryVersion", playVersion)
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
    testImplementation("com.typesafe.play", "play-caffeine-cache_$scalaBinaryVersion", playVersion)
    testImplementation("com.willowtreeapps.assertk", "assertk-jvm", Versions.assertk)
}

sourceSets.main {
    java.srcDirs("src/main/kotlin", "src/main/kotlin-$scalaBinaryVersion")
}

sourceSets.test {
    java.srcDirs("src/test/kotlin-$scalaBinaryVersion")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get())
}

ktlint {
    version.set(Versions.ktlint)
    outputToConsole.set(true)
    reporters {
        reporter(ReporterType.CHECKSTYLE)
    }
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
    from(tasks.dokkaJavadoc)
}

tasks.dokkaJavadoc.configure {
    outputDirectory.set(buildDir.resolve("javadoc"))
    dokkaSourceSets {
        configureEach {
            jdkVersion.set(8)
            reportUndocumented.set(false)
            externalDocumentationLink {
                url.set(URL("https://www.lagomframework.com/documentation/1.6.x/java/api/"))
            }
            externalDocumentationLink {
                url.set(URL("https://static.javadoc.io/com.google.guava/guava/26.0-jre/"))
            }
            displayName.set("JVM")
        }
    }
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
    useGpgCmd()
    isRequired = isRelease
    sign(publishing.publications["maven"])
}

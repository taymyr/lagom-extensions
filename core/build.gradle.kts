@file:Suppress("UnstableApiUsage")

plugins {
    scala
    id("cz.alenkacz.gradle.scalafmt") version Versions.`scalafmt-plugin`
    `maven-publish`
    signing
}

dependencies {
    compileOnly("com.typesafe.play", "play-ahc-ws_$scalaBinaryVersion", playVersion)

    testImplementation("org.scalatest", "scalatest_$scalaBinaryVersion", Versions.scalatest)
}

configurations {
    testCompile.get().extendsFrom(compileOnly.get())
}

val scalaTest by tasks.creating(JavaExec::class) {
    main = "org.scalatest.tools.Runner"
    args = listOf("-R", "$buildDir/classes/scala/test", "-o")
    classpath = sourceSets.test.get().runtimeClasspath
    dependsOn(tasks.testClasses)
}
tasks.test { dependsOn(scalaTest) }
jacoco { applyTo(scalaTest) }

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val scalaDocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.scaladoc)
}

tasks.check { dependsOn(tasks.checkScalafmtAll) }

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "${project.name}_$scalaBinaryVersion"
            from(components["java"])
            artifact(sourcesJar)
            artifact(scalaDocJar)
            pom(Publishing.pom)
        }
    }
}

signing {
    isRequired = isRelease
    sign(publishing.publications["maven"])
}
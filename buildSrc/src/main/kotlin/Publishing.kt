import org.gradle.api.publish.maven.MavenPom

@Suppress("UnstableApiUsage")
object Publishing {
    val pom: (MavenPom).() -> Unit = {
        name.set("Lagom Extensions")
        description.set("Utilities for Lagom framework")
        url.set("https://taymyr.org")
        organization {
            name.set("Digital Economy League")
            url.set("https://www.digitalleague.ru/")
        }
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("taymyr")
                name.set("Taymyr Contributors")
                email.set("contributors@taymyr.org")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/taymyr/lagom-extensions.git")
            developerConnection.set("scm:git:https://github.com/taymyr/lagom-extensions.git")
            url.set("https://github.com/taymyr/lagom-extensions")
            tag.set("HEAD")
        }
    }
}
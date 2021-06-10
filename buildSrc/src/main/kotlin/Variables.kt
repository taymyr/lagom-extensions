import org.gradle.api.Project

val Project.isRelease get() = !this.version.toString().endsWith("SNAPSHOT")

val Project.lagomVersion get() = this.properties["lagomVersion"] as String? ?: Versions.lagom
val Project.playVersion get() = this.properties["playVersion"] as String? ?: Versions.play
val Project.scalaBinaryVersion get() = this.properties["scalaBinaryVersion"] as String? ?: Versions.scalaBinary

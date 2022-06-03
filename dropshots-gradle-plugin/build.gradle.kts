import com.vanniktech.maven.publish.SonatypeHost.S01
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-gradle-plugin`
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.binaryCompatibilityValidator)
}

buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

sourceSets {
  main.configure {
    java.srcDir("src/generated/kotlin")
  }
}

val generateVersionTask = tasks.register("generateVersion") {
  inputs.property("version", project.property("VERSION_NAME") as String)
  outputs.dir(project.layout.projectDirectory.dir("src/generated/kotlin"))

  doLast {
    val output = File(outputs.files.first(), "com/dropbox/dropshots/Version.kt")
    output.parentFile.mkdirs()
    output.writeText("""
      |// Generated by gradle task.
      |package com.dropbox.dropshots
      |public const val VERSION: String = "${inputs.properties["version"]}"
    """.trimMargin())
  }
}

tasks.named("compileKotlin").configure {
  dependsOn(generateVersionTask)
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "1.8"
    // Because Gradle's Kotlin handling is stupid, this falls out of date quickly
    apiVersion = "1.5"
    languageVersion = "1.5"

    // We use class SAM conversions because lambdas compiled into invokedynamic are not
    // Serializable, which causes accidental headaches with Gradle configuration caching. It's
    // easier for us to just use the previous anonymous classes behavior
    @Suppress("SuspiciousCollectionReassignment")
    freeCompilerArgs += "-Xsam-conversion=class"
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(8)
}

kotlin {
  explicitApi()
}

gradlePlugin {
  plugins {
    plugins.create("dropshots") {
      id = "com.dropbox.dropshots"
      implementationClass = "com.dropbox.dropshots.DropshotsPlugin"
    }
  }
}

dependencies {
  compileOnly(gradleApi())
  implementation(platform(libs.kotlin.bom))
  // Don't impose our version of KGP on consumers
  compileOnly(libs.kotlin.plugin)
  compileOnly(libs.android)

  testImplementation(gradleTestKit())
  testImplementation(platform(libs.kotlin.bom))
  testImplementation(libs.kotlin.plugin)
  testImplementation(libs.android)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

tasks.register("printVersionName") {
  doLast {
    val VERSION_NAME: String by project
    println(VERSION_NAME)
  }
}

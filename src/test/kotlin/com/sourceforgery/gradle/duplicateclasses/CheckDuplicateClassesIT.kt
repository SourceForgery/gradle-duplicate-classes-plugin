package com.sourceforgery.gradle.duplicateclasses

import com.sourceforgery.gradle.duplicateclasses.CheckDuplicateClassesPlugin.Companion.pluginId
import com.sourceforgery.gradle.duplicateclasses.CheckDuplicateClassesPlugin.Companion.taskName
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CheckDuplicateClassesIT {

    lateinit var buildFile: File
    lateinit var gradleRunner: GradleRunner

    @BeforeEach
    fun setup(@TempDir testProjectDir: File) {
        buildFile = File(testProjectDir, "build.gradle.kts")
        buildFile.writeText("""
            |plugins {
            |    `java`
            |    id("$pluginId")
            |}
            |repositories {
            |    jcenter()
            |}
            |
        """.trimMargin())

        gradleRunner = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("build")
    }


    fun ignoreFailures() {
        buildFile.appendText("""
            |(tasks["$taskName"] as ${CheckDuplicateClassesTask::class.java.name}).setIgnoreFailures(true)
            |
        """.trimMargin())
    }


    @Test
    fun `no duplicates`() {
        buildFile.appendText("""
            dependencies {
                implementation("javax.annotation:javax.annotation-api:1.3.2")
                implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")
            }
        """.trimIndent())
        gradleRunner.build()
    }

    @Test
    fun `no duplicates with ignore failures`() {
        ignoreFailures()
        buildFile.appendText("""
            dependencies {
                implementation("javax.annotation:javax.annotation-api:1.3.2")
                implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")
            }
        """.trimIndent())
        gradleRunner.build()
    }

    @Test
    fun `duplicates with ignore failures`() {
        ignoreFailures()
        buildFile.appendText("""
            dependencies {
                implementation("javax.annotation:javax.annotation-api:1.3.2")
                implementation("jakarta.annotation:jakarta.annotation-api:1.3.5")
            }
        """.trimIndent())
        gradleRunner.build()
    }

    @Test
    fun duplicates() {
        buildFile.appendText("""
            dependencies {
                implementation("javax.annotation:javax.annotation-api:1.3.2")
                implementation("jakarta.annotation:jakarta.annotation-api:1.3.5")
            }
        """.trimIndent())
        gradleRunner.buildAndFail()
    }
}

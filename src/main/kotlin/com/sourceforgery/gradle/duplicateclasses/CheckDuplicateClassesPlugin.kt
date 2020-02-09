package com.sourceforgery.gradle.duplicateclasses

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering

class CheckDuplicateClassesPlugin : Plugin<Project> {
    @Suppress("UnstableApiUsage")
    override fun apply(target: Project) {
        with(target) {
            extensions.create<CheckDuplicateClassesExtension>("com.sourceforgery.duplicateClassesChecker")
            with(tasks) {
                val checkDuplicateClasses by registering(CheckDuplicateClassesTask::class)
                val byName = findByName("check")
                        ?: error("com.sourceforgery.check-duplicate-classes needs to be applied after the jar-producing plugin, e.g. java or kotlin")
                byName.dependsOn(checkDuplicateClasses)
            }
        }
    }

    companion object {
        const val pluginId = "com.sourceforgery.check-duplicate-classes"
        const val taskName = "checkDuplicateClasses"
    }
}

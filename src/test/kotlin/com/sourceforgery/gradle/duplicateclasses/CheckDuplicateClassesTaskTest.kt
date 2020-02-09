package com.sourceforgery.gradle.duplicateclasses

import com.sourceforgery.gradle.duplicateclasses.CheckDuplicateClassesPlugin.Companion.pluginId
import com.sourceforgery.gradle.duplicateclasses.CheckDuplicateClassesPlugin.Companion.taskName
import org.gradle.api.GradleException
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertThrows


class CheckDuplicateClassesTaskTest {
    @Test
    fun `check that task is applied`(testInfo: TestInfo) {
        val project = ProjectBuilder.builder().withName(testInfo.displayName).build()
        project.pluginManager.apply("java")
        project.pluginManager.apply("com.sourceforgery.check-duplicate-classes")

        val task = project.tasks.findByName(taskName)
            ?: fail()
        assertTrue(task is CheckDuplicateClassesTask)
        for (action in task.actions) {
            action.execute(task)
        }
    }

    @Test
    fun `ensure fails without check task`(testInfo: TestInfo) {
        val project = ProjectBuilder.builder().withName(testInfo.displayName).build()
        assertThrows<PluginApplicationException> {
            project.pluginManager.apply(pluginId)
        }
    }

    @Test
    fun `no duplicates is good`(testInfo: TestInfo) {
        val project = ProjectBuilder.builder().withName(testInfo.displayName).build()
        project.repositories.jcenter()
        project.pluginManager.apply("java")
        project.pluginManager.apply(pluginId)

        project.dependencies.add("implementation", "javax.annotation:javax.annotation-api:1.3.2")

        val task = project.tasks.findByName(taskName)
            ?: fail()
        for (action in task.actions) {
            action.execute(task)
        }
    }

    @Test
    fun `throw when duplicates are found`(testInfo: TestInfo) {
        val project = ProjectBuilder.builder().withName(testInfo.displayName).build()
        project.repositories.jcenter()
        project.pluginManager.apply("java")
        project.pluginManager.apply(pluginId)

        project.dependencies.add("implementation", "javax.annotation:javax.annotation-api:1.3.2")
        project.dependencies.add("implementation", "jakarta.annotation:jakarta.annotation-api:1.3.5")

        val task = project.tasks.findByName(taskName)
            ?: fail()
        assertThrows<GradleException> {
            for (action in task.actions) {
                action.execute(task)
            }
        }
    }
}

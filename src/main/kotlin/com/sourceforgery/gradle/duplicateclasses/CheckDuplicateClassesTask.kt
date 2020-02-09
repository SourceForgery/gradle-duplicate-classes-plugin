package com.sourceforgery.gradle.duplicateclasses

import checkDuplicateClasses
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import org.gradle.api.plugins.JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask
import org.gradle.api.tasks.compile.AbstractCompile
import java.io.File
import java.util.zip.ZipFile

/**
 * Checks whether the artifacts of the configurations of the project contain duplicate classes.
 */
open class CheckDuplicateClassesTask : DefaultTask(), VerificationTask {
    private var ignoreFailures: Boolean = false

    override fun getIgnoreFailures() = this.ignoreFailures
    override fun setIgnoreFailures(ignoreFailures: Boolean) {
        this.ignoreFailures = ignoreFailures
    }

    private val extension by lazy(LazyThreadSafetyMode.NONE) {
        project.checkDuplicateClasses
    }

    init {
        getCompileJava()?.let {
            dependsOn(it)
        }

        getCompileTestJava()?.let {
            dependsOn(it)
        }
    }

    private fun getCompileJava(): AbstractCompile? =
        project.tasks.findByName(COMPILE_JAVA_TASK_NAME) as? AbstractCompile

    private fun getCompileTestJava(): AbstractCompile? =
        project.tasks.findByName(COMPILE_TEST_JAVA_TASK_NAME) as? AbstractCompile

    @TaskAction
    fun checkForDuplicateClasses() {
        val result = StringBuilder()

        result.append(checkConfiguration(getCompileJava()))
        result.append(checkConfiguration(getCompileTestJava()))

        if (result.isNotEmpty()) {
            val message = StringBuilder("There are conflicting files in the following tasks")

            if (project.gradle.startParameter.logLevel > LogLevel.INFO) {
                message.append(" (add --info for details)")
            }

            message.append(":$result")

            if (ignoreFailures) {
                logger.warn(message.toString())
            } else {
                throw GradleException(message.toString())
            }
        }
    }

    private fun checkConfiguration(compileTask: AbstractCompile?): String {
        if (compileTask == null) {
            return ""
        }

        logger.info("Checking for duplicates in ${compileTask.path}")
        val classpath = compileTask.classpath.toList() + compileTask.outputs.files

        val jarsByFile = classPathToGroupByRelativePath(classpath)

        val jarsAllowedToContainDuplicates = SimpleMultimap<String, String>()
        for (depList in extension.acceptedDuplicates) {
            for (dep in depList) {
                jarsAllowedToContainDuplicates[dep] = depList.toTreeSet()
            }
        }

        val duplicateFiles = jarsByFile
            .entries
            .filter { (_, archiveSet) ->
                // Multiple jars for the file
                archiveSet.size > 1
            }
            // Filter out all files where ALL dupes are listed
            .filterNot { (_, jars) ->
                val values = jars.map { jar -> File(jar).nameWithoutExtension }
                jarsAllowedToContainDuplicates[jars.first()]
                    ?.containsAll(values)
                    ?: false
            }
            .associateTo(SimpleMultimap()) { (file, jars) ->
                file to jars
            }

        if (duplicateFiles.size != 0) {
            logger.info(buildMessageWithConflictingClasses(duplicateFiles))
            return "\n\n${compileTask.path}\n${buildMessageWithUniqueModules(duplicateFiles.values)}"
        }

        return ""
    }

    /**
     * If the Multimap contains more than one ArchiveFile for any RelativePath,
     * we have duplicates
     */
    private fun classPathToGroupByRelativePath(classpath: List<File>): SimpleMultimap<RelativePath, ArchiveFile> {
        val ignoredFiles = extension.ignoredFiles
        val ignoredExtensions = extension.ignoredExtensions

        return classpath.asSequence().flatMap { entry ->
            logger.debug("    '$entry'")

            if (entry.isFile && (
                            entry.name.endsWith("zip") ||
                                    entry.name.endsWith("jar")
                            )
            ) {
                mapZipFile(entry, ignoredFiles)
            } else if (entry.isDirectory) {
                mapDirectory(entry, ignoredFiles)
            } else {
                if (entry.exists() && !ignoredExtensions.contains(entry.extension)) {
                    logger.warn("Don't know what to do with dependency $entry")
                    emptySequence()
                } else {
                    emptySequence()
                }
            }
        }.toSimpleMap()
    }

    private fun mapDirectory(entry: File, ignoredFiles: MutableList<Regex>): Sequence<Pair<RelativePath, ArchiveFile>> {
        val directory = entry.absoluteFile.canonicalFile
        return directory.walk()
            .filterNot { it.isDirectory }
            .map { it.relativeTo(directory).path }
            .filterPaths(ignoredFiles)
            .map { it to entry.path }
    }

    private fun mapZipFile(entry: File, ignoredFiles: MutableList<Regex>): Sequence<Pair<RelativePath, ArchiveFile>> =
        try {
            ZipFile(entry).use { zip ->
                zip.entries()
                    .asSequence()
                    .filterNot { it.isDirectory }
                    .map { it.name }
                    .filterPaths(ignoredFiles)
                    .toList()
                    .asSequence()
                    .map { it to entry.path }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to open zip $entry", e)
        }

    fun Sequence<RelativePath>.filterPaths(ignoredFiles: List<Regex>) = this
        .filter { it.endsWith(".class") }
        .filter {
            ignoredFiles.none { regex ->
                regex.matches(it)
            }
        }

    private fun buildMessageWithConflictingClasses(duplicateFiles: SimpleMultimap<String, String>): String {
        val conflictingClasses = SimpleMultimap<String, String>()

        duplicateFiles
                .entries
                .forEach { (clazz, list) ->
                    list.forEach {
                        conflictingClasses[it] = clazz
                    }
                }
        val message = StringBuilder()
        conflictingClasses.forEach {
            message.append("\n    Found duplicate classes in ${it.key}:\n        ${it.value.joinToString("\n        ")}")
        }

        return message.toString()
    }

    private fun buildMessageWithUniqueModules(conflictingModules: Collection<Collection<String>>): String {
        val moduleMessages = mutableListOf<String>()

        conflictingModules.forEach { modules ->
            val message = "    ${joinModules(modules)}"
            if (!moduleMessages.contains(message)) {
                moduleMessages.add(message)
            }
        }

        return moduleMessages.joinToString("\n")
    }

    private fun joinModules(modules: Collection<String>): String {
        return modules.joinToString(", ")
    }
}
/** Jar or directory that is in class path */
private typealias ArchiveFile = String
/** Path relative to the ArchiveFile (in case of directory) or pathname in the jar */
private typealias RelativePath = String

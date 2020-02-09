package com.sourceforgery.gradle.duplicateclasses

open class CheckDuplicateClassesExtension {
    // List of allowed duplicates , e.g.
    // acceptedDuplicates = mutableListOf(
    //     mutableListOf("mongodb-driver-3.6.1", "fongo-2.2.0-RC2")
    // )
    var acceptedDuplicates: MutableList<MutableList<String>> = mutableListOf(mutableListOf())

    // Files that are allowed to collide because they are sometimes even supposed to
    var ignoredFiles = mutableListOf(
        Regex("^module-info.class$"),
        Regex(".*/package-info.class$"),
        Regex("^META-INF/.*")
    )

    var ignoredExtensions = mutableSetOf(
        "exe",
        "gz",
        "tar"
    )
}

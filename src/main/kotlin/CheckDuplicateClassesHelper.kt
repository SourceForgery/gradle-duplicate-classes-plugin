import com.sourceforgery.gradle.duplicateclasses.CheckDuplicateClassesExtension
import org.gradle.api.Project

@Suppress("UnstableApiUsage")
val Project.checkDuplicateClasses: CheckDuplicateClassesExtension
    get() =
        extensions.findByType(CheckDuplicateClassesExtension::class.java)
            ?: error("Apply duplicate classes extension first")

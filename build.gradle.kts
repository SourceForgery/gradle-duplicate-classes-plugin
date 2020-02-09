import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    id("net.researchgate.release") version "2.8.1"
}

group = "com.sourceforgery.gradle-duplicate-classes-plugin"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}

gradlePlugin {
    @Suppress("UnstableApiUsage")
    (plugins) {
        register("check-duplicate-classes-plugin") {
            id = "com.sourceforgery.check-duplicate-classes"
            implementationClass = "com.sourceforgery.gradle.duplicateclasses.CheckDuplicateClassesPlugin"
            displayName = "Check For Duplicate Classes"
            description = """
                |Check classpaths for duplicate files to avoid having 
                |multiple dependencies with the exact same class. e.g. 
                |"com.google.code.findbugs:annotations" and "com.google.code.findbugs:jsr305".
                |The plugin will fail builds with classpath collisions.
            """.trimMargin()
        }
    }
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }

    test {
        useJUnitPlatform()
    }
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

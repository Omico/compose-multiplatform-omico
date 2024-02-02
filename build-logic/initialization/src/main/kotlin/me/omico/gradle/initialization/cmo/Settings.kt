package me.omico.gradle.initialization.cmo

import me.omico.cmo.CmoDependency
import me.omico.cmo.resolveCmoRootLocalProperties
import org.gradle.api.initialization.Settings
import java.io.File

internal fun Settings.includeCmoSubprojects() {
    val properties = resolveCmoRootLocalProperties(rootDir, providers)
    fetchDependenciesFromJetpackComposeBom(properties.jetpack.compose.bom.version)
        .forEach { dependency -> include(dependency) }
}

private fun Settings.include(dependency: CmoDependency) {
    val gradleProjectPath = dependency.gradleProjectPath
    val moduleDirectory = cmoModuleDirectoryFor(dependency)
    generateDefaultCmoModuleBuildScript(moduleDirectory)
    generateDefaultCmoModuleGitIgnoreFile(moduleDirectory)
    include(gradleProjectPath)
    project(gradleProjectPath).run {
        name = dependency.artifactId
        projectDir = moduleDirectory
    }
}

// TODO generateDefaultCmoBuildScript should read & transform directly from the Androidx repository via Kotlin PSI.
private fun generateDefaultCmoModuleBuildScript(moduleDirectory: File) {
    val gradleBuildScript = moduleDirectory.resolve("build.gradle.kts")
    if (gradleBuildScript.exists()) return
    gradleBuildScript.writeText(
        """
        |plugins {
        |    id("cmo.compose")
        |}
        |
        |cmo {
        |    compose {
        |        applyDefaultTargets()
        |    }
        |}
        |
        |kotlin {
        |    sourceSets {
        |        commonMain {
        |            dependencies {
        |            }
        |        }
        |    }
        |}
        |
        """.trimMargin(),
    )
}

private fun generateDefaultCmoModuleGitIgnoreFile(moduleDirectory: File) {
    val gitIgnoreFile = moduleDirectory.resolve(".gitignore")
    if (gitIgnoreFile.exists()) return
    gitIgnoreFile.writeText(
        """
        |/build
        |
        """.trimMargin(),
    )
}

package me.omico.gradle.project.cmo.internal

import gradle.kotlin.dsl.accessors._365d05c1ec31bf356526151902bb28a5.operations
import me.omico.cmo.readProperties
import me.omico.consensus.dsl.consensus
import me.omico.consensus.git.ConsensusGitExtension
import me.omico.gradle.project.cmo.CmoComposeModuleExtension
import me.omico.gradle.project.cmo.CmoExtension
import me.omico.gradle.project.cmo.CmoModuleProperties
import me.omico.gradle.project.cmo.tasks.RefreshJetpackComposeCommitId
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import java.io.File

internal fun Project.configureCmoCompose() {
    val cmoExtension = extensions.create("cmo", CmoExtension::class)
    val cmoComposeModuleExtension = cmoExtension.extensions.create(
        publicType = CmoComposeModuleExtension::class,
        name = "compose",
        instanceType = CmoComposeModuleExtensionImpl::class,
    )
    with(cmoComposeModuleExtension as CmoComposeModuleExtensionImpl) {
        scope = projectDir.parentFile.parentFile.name
        groupId = "androidx.compose.$scope"
        artifactId = projectDir.parentFile.name
        version = projectDir.name
        file("cmo.properties").takeIf(File::exists)
            ?.readProperties<CmoModuleProperties>()
            ?.let { jetpackComposeCommitId = it.jetpack.compose.commitId }
    }
    group = cmoComposeModuleExtension.groupId
    version = cmoComposeModuleExtension.version
    tasks.register<RefreshJetpackComposeCommitId>("refreshJetpackComposeCommitId") {
        groupIdProperty.set(cmoComposeModuleExtension.groupId)
        artifactIdProperty.set(cmoComposeModuleExtension.artifactId)
        versionProperty.set(cmoComposeModuleExtension.version)
        cmoPropertiesFileProperty.set(layout.projectDirectory.file("cmo.properties"))
    }
}

internal fun Project.syncFromJetpackComposeRepository(jetpackComposeSourceDirectory: File) {
    val consensusGitExtension = rootProject.consensus.extensions.getByType<ConsensusGitExtension>()
    val cmoComposeModuleExtension = project.cmoComposeModuleExtension
    val outputDirectory = projectDir.resolve("src")
    if (outputDirectory.exists()) outputDirectory.deleteRecursively()
    outputDirectory.mkdirs()
    consensusGitExtension.operations {
        val currentCommitId = exec(
            "git", "rev-parse", "HEAD",
            workingDir = jetpackComposeSourceDirectory,
        )
        if (currentCommitId == cmoComposeModuleExtension.jetpackComposeCommitId) return@operations
        exec(
            "git", "checkout", cmoComposeModuleExtension.jetpackComposeCommitId,
            workingDir = jetpackComposeSourceDirectory,
        )
    }
    jetpackComposeSourceDirectory
        .resolve("${cmoComposeModuleExtension.scope}/${cmoComposeModuleExtension.artifactId}/src")
        .copyRecursively(outputDirectory)
}

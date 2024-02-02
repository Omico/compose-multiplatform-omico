package me.omico.gradle.project.cmo.tasks

import me.omico.cmo.CmoDependency
import me.omico.cmo.fetchJetpackComposeModuleCommitId
import me.omico.cmo.writeProperties
import me.omico.gradle.project.cmo.CmoModuleProperties
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile

internal abstract class RefreshJetpackComposeCommitId : CmoTask() {
    @get:Input
    abstract val groupIdProperty: Property<String>

    @get:Input
    abstract val artifactIdProperty: Property<String>

    @get:Input
    abstract val versionProperty: Property<String>

    @get:OutputFile
    abstract val cmoPropertiesFileProperty: RegularFileProperty

    override fun execute() {
        val dependency = CmoDependency(
            groupId = groupIdProperty.get(),
            artifactId = artifactIdProperty.get(),
            version = versionProperty.get(),
        )
        val cmoPropertiesFile = cmoPropertiesFileProperty.get().asFile
        val commitId = dependency.fetchJetpackComposeModuleCommitId()
        if (commitId == null) {
            logger.warn("Failed to fetch Jetpack Compose module commit id for $dependency.")
            return
        }
        val properties = CmoModuleProperties(jetpackComposeCommitId = commitId)
        cmoPropertiesFile.writeProperties(properties)
    }
}

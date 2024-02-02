package me.omico.gradle.project.cmo

interface CmoComposeModuleExtension {
    val scope: String
    val groupId: String
    val artifactId: String
    val version: String
    val jetpackComposeCommitId: String

    fun applyDefaultTargets()
}

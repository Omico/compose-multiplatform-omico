import me.omico.cmo.resolveCmoRootLocalProperties
import me.omico.gradle.project.cmo.internal.syncFromJetpackComposeRepository

plugins {
    id("cmo.root.git")
    id("cmo.root.spotless")
}

val cmoRootLocalProperties = resolveCmoRootLocalProperties(rootDir, providers)
val jetpackComposeSourceDirectory = file(cmoRootLocalProperties.jetpack.repository.path).resolve("compose")

val isGitRepoDirty = consensus.git.operations.isDirty()
val hasSyncFromJetpackComposeRepositoryTask =
    gradle.startParameter.taskNames.any { taskName -> "syncFromJetpackComposeRepository" in taskName }
val canRunSyncFromJetpackComposeRepositoryTask = isGitRepoDirty.not() && hasSyncFromJetpackComposeRepositoryTask
if (isGitRepoDirty && hasSyncFromJetpackComposeRepositoryTask) {
    throw GradleException(
        "Git repository is dirty. " +
            "Please commit or stash your changes before run [syncFromJetpackComposeRepository] task.",
    )
}

allprojects {
    plugins.withId("cmo.compose") {
        tasks.register("syncFromJetpackComposeRepository") {
            onlyIf { canRunSyncFromJetpackComposeRepositoryTask }
            doLast {
                if (gradle.startParameter.currentDir != projectDir) {
                    throw GradleException(
                        "The [syncFromJetpackComposeRepository] task must be run from a module directory.",
                    )
                }
                syncFromJetpackComposeRepository(jetpackComposeSourceDirectory)
            }
        }
    }
}

tasks.register("syncAllFromJetpackComposeRepository") {
    doLast {
        allprojects {
            if (pluginManager.hasPlugin("cmo.compose").not()) return@allprojects
            syncFromJetpackComposeRepository(jetpackComposeSourceDirectory)
        }
    }
}

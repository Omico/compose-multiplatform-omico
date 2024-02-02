package me.omico.gradle.initialization.cmo

import me.omico.cmo.CmoDependency
import org.gradle.api.initialization.Settings
import java.io.File

fun Settings.cmoModuleDirectoryFor(dependency: CmoDependency): File =
    rootDir.resolve("cmo/${dependency.scope}/${dependency.artifactId}/${dependency.version}").also(File::mkdirs)

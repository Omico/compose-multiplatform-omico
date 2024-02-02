package me.omico.gradle.initialization.cmo

import me.omico.cmo.CmoDependency
import me.omico.cmo.fetchDependenciesFromJetpackComposeBom
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.file.FileOperations
import org.gradle.kotlin.dsl.support.serviceOf

internal fun Settings.fetchDependenciesFromJetpackComposeBom(version: String): Iterable<CmoDependency> =
    serviceOf<FileOperations>().resources.fetchDependenciesFromJetpackComposeBom(version)

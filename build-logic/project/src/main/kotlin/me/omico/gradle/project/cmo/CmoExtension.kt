package me.omico.gradle.project.cmo

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType

interface CmoExtension : ExtensionAware

internal val Project.cmoExtension: CmoExtension
    get() = extensions.getByType()

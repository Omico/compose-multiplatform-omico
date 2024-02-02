package me.omico.gradle.project.cmo.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

internal abstract class CmoTask : DefaultTask() {
    init {
        group = GROUP
    }

    @TaskAction
    protected open fun execute(): Unit = Unit

    companion object {
        const val GROUP = "cmo"
    }
}

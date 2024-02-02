package me.omico.gradle.project.cmo.internal

import me.omico.gradle.project.cmo.CmoComposeModuleExtension
import me.omico.gradle.project.cmo.cmoExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal abstract class CmoComposeModuleExtensionImpl(
    private val project: Project,
) : CmoComposeModuleExtension {
    override var scope: String = ""
    override var groupId: String = ""
    override var artifactId: String = ""
    override var version: String = ""
    override var jetpackComposeCommitId: String = ""

    init {
        configureKotlinMultiplatform {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
                freeCompilerArgs.add("-Xjvm-default=all")
            }
        }
    }

    override fun applyDefaultTargets() {
        configureKotlinMultiplatform {
            androidTarget {
                publishLibraryVariants("release")
            }
            jvm("desktop")

            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            applyDefaultHierarchyTemplate {
                common {
                    group("jvm") {
                        withAndroidTarget()
                        withJvm()
                    }
                }
            }
        }
    }

    private fun configureKotlinMultiplatform(block: KotlinMultiplatformExtension.() -> Unit): Unit =
        project.extensions.configure(block)
}

internal val Project.cmoComposeModuleExtension: CmoComposeModuleExtension
    get() = cmoExtension.extensions.getByType()

import me.omico.gradle.initialization.cmo.includeCmoSubprojects
import me.omico.gradm.addDeclaredRepositories

addDeclaredRepositories()

plugins {
    id("cmo.gradm")
    id("cmo.gradle-enterprise")
}

includeBuild("build-logic/project")

includeCmoSubprojects()

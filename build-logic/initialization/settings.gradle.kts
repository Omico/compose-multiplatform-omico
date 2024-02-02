rootProject.name = "cmo-initialization"

pluginManagement {
    includeBuild("../gradm")
}

plugins {
    id("cmo.gradm")
}

includeBuild("../shared") {
    dependencySubstitution {
        substitute(module("me.omico.cmo:cmo-shared")).using(project(":"))
    }
}

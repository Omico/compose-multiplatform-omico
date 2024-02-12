plugins {
    id("cmo.compose")
}

cmo {
    compose {
        applyDefaultTargets()
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":cmo:runtime:runtime"))
                implementation(project(":cmo:ui:ui-util"))
            }
        }
    }
}

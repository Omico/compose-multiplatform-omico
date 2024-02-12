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
                api(project(":cmo:animation:animation-core"))
                api(project(":cmo:foundation:foundation-layout"))
                api(project(":cmo:runtime:runtime"))
                api(project(":cmo:ui:ui"))
                api(project(":cmo:ui:ui-geometry"))
                implementation(project(":cmo:ui:ui-util"))
                implementation("androidx.collection:collection:1.4.0")
            }
        }
    }
}

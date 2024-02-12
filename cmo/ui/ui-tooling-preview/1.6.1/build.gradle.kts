plugins {
    id("cmo.compose")
}

cmo {
    compose {
        applyDefaultTargets()
    }
}

//noinspection GradleDependency
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":cmo:runtime:runtime"))
            }
        }
        androidMain {
            dependencies {
                api("androidx.annotation:annotation:1.2.0")
            }
        }
    }
}

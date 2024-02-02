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
            }
        }
    }
}

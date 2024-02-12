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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
                implementation("androidx.collection:collection:1.4.0")
            }
        }

        androidMain {
            dependencies {
                api("androidx.annotation:annotation-experimental:1.4.0")
            }
        }
    }
}

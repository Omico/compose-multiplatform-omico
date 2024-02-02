import me.omico.consensus.dsl.requireRootProject
import me.omico.consensus.spotless.ConsensusSpotlessTokens

plugins {
    id("me.omico.consensus.spotless")
}

requireRootProject()

consensus {
    spotless {
        rootProject {
            freshmark(
                excludeTargets = setOf(
                    "cmo/**/*.md",
                ),
            )
            gradleProperties()
        }
        allprojects {
            kotlin(
                targets = ConsensusSpotlessTokens.Kotlin.targets + setOf(
                    "build-logic/**/src/main/kotlin/**/*.kt",
                ),
                excludeTargets = setOf(
                    "src/*/*/androidx/compose/**/*.kt",
                ),
            )
            kotlinGradle()
        }
    }
}

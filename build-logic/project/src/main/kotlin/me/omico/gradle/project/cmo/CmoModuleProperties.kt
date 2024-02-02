package me.omico.gradle.project.cmo

import kotlinx.serialization.Serializable

@Serializable
data class CmoModuleProperties(
    val jetpack: Jetpack = Jetpack(),
) {
    @Serializable
    data class Jetpack(
        val compose: Compose = Compose(),
    ) {
        @Serializable
        data class Compose(
            val commitId: String = "",
        )
    }
}

internal fun CmoModuleProperties(jetpackComposeCommitId: String): CmoModuleProperties =
    CmoModuleProperties(
        jetpack = CmoModuleProperties.Jetpack(
            compose = CmoModuleProperties.Jetpack.Compose(
                commitId = jetpackComposeCommitId,
            ),
        ),
    )

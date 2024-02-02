package me.omico.cmo

import kotlinx.serialization.Serializable
import org.gradle.api.provider.ProviderFactory
import java.io.File

@Serializable
data class CmoRootLocalProperties(
    val jetpack: Jetpack = Jetpack(),
) {
    @Serializable
    data class Jetpack(
        val compose: Compose = Compose(),
        val repository: Repository = Repository(),
    ) {
        @Serializable
        data class Compose(
            val bom: Bom = Bom(),
        ) {
            @Serializable
            data class Bom(
                val version: String = "",
            )
        }

        @Serializable
        data class Repository(
            val path: String = "",
        )
    }
}

fun resolveCmoRootLocalProperties(rootDir: File, providers: ProviderFactory): CmoRootLocalProperties {
    val cmoRootLocalPropertiesFile = rootDir.resolve("cmo.properties")
    var properties = CmoRootLocalProperties()
    if (cmoRootLocalPropertiesFile.exists()) properties = cmoRootLocalPropertiesFile.readProperties()
    if (properties.jetpack.compose.bom.version.isBlank()) {
        val defaultCmoJetpackComposeBomVersion = providers.gradleProperty("cmo.jetpack.compose.bom.version").get()
        properties = CmoRootLocalProperties(defaultCmoJetpackComposeBomVersion)
        cmoRootLocalPropertiesFile.writeProperties(properties)
    }
    return properties
}

private fun CmoRootLocalProperties(defaultCmoJetpackComposeBomVersion: String): CmoRootLocalProperties =
    CmoRootLocalProperties(
        jetpack = CmoRootLocalProperties.Jetpack(
            compose = CmoRootLocalProperties.Jetpack.Compose(
                bom = CmoRootLocalProperties.Jetpack.Compose.Bom(
                    version = defaultCmoJetpackComposeBomVersion,
                ),
            ),
        ),
    )

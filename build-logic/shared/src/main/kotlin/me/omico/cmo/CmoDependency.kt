package me.omico.cmo

import org.apache.maven.api.model.Dependency

data class CmoDependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
) {
    val module: String = "$groupId:$artifactId"
    val scope: String = groupId.removePrefix("androidx.compose.")
    val gradleProjectPath = ":cmo:$scope:$artifactId"
    val pomPath = "${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.pom"

    override fun toString(): String = "$module:$version"
}

internal fun Dependency.toCmoDependency(): CmoDependency =
    CmoDependency(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
    )

internal fun Iterable<Dependency>.toCmoDependencies(): Iterable<CmoDependency> = map(Dependency::toCmoDependency)

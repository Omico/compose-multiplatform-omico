package me.omico.cmo

import org.apache.maven.api.model.Model
import org.apache.maven.internal.impl.DefaultModelXmlFactory
import org.gradle.api.resources.ResourceHandler
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

fun ResourceHandler.fetchJetpackComposePomModel(pomPath: String): Model =
    fetchGoogleMavenRepository(pomPath).let(DefaultModelXmlFactory::fromXml)

fun ResourceHandler.fetchJetpackComposePomModel(dependency: CmoDependency): Model =
    fetchJetpackComposePomModel(dependency.pomPath)

fun ResourceHandler.fetchDependenciesFromJetpackComposeBom(version: String): Iterable<CmoDependency> =
    fetchJetpackComposePomModel("androidx/compose/compose-bom/$version/compose-bom-$version.pom")
        .dependencyManagement.dependencies
        .toCmoDependencies()
        .filterNot(CmoDependency::isExcluded)

fun CmoDependency.fetchJetpackComposeModuleCommitId(): String? {
    val url = "https://developer.android.com/jetpack/androidx/releases/compose-$scope"
    val body = Jsoup.connect(url).get().body()
    val buildListElement = body.selectFirst("div.devsite-article-body") ?: return null
    return buildListElement
        .getElementsContainingOwnText("Version $version contains these commits.")
        .firstNotNullOfOrNull(::fetchJetpackComposeModuleCommitId)
}

private fun CmoDependency.fetchJetpackComposeModuleCommitId(releaseElement: Element?): String? {
    if (releaseElement == null) {
        println("No release element found for $this")
        return null
    }
    val releaseDescription = releaseElement.parent()?.text()
    if (releaseDescription == null) {
        println("No release description found for $this")
        return null
    }
    // Condition 1:
    //
    // androidx.compose.material3:material3:1.1.2 and androidx.compose.material3:material3-window-size-class:1.1.2
    // are released. Version 1.1.2 contains these commits.
    //
    // Condition 2:
    //
    // androidx.compose.ui:ui-*:1.6.0 is released. Version 1.6.0 contains these commits.
    //
    if (
        toString() !in releaseDescription &&
        "androidx.compose.$scope:$scope-*:$version" !in releaseDescription
    ) {
        println("Cannot match release description for $this")
        return null
    }
    val commitsUrl = releaseElement.attr("href")
    val commitsUrlRegex =
        "https://android.googlesource.com/platform/frameworks/support/\\+log/[a-f0-9]{40}\\.\\.([a-f0-9]{40}).*"
            .toRegex()
    val commitId = commitsUrlRegex.matchEntire(commitsUrl)?.groupValues?.get(1)
    if (commitId == null) {
        println("No commit id found for $this")
        return null
    }
    return commitId
}

private fun CmoDependency.isExcluded(): Boolean =
    artifactId.endsWith("-android") || artifactId.endsWith("-desktop") || module in excludedModules

private val excludedModules: Set<String> = setOf(
    "androidx.compose.runtime:runtime-livedata",
    "androidx.compose.runtime:runtime-rxjava2",
    "androidx.compose.runtime:runtime-rxjava3",
    "androidx.compose.ui:ui-android-stubs",
    "androidx.compose.ui:ui-test",
    "androidx.compose.ui:ui-test-junit4",
    "androidx.compose.ui:ui-test-manifest",
    "androidx.compose.ui:ui-text-google-fonts",
    "androidx.compose.ui:ui-viewbinding",
)

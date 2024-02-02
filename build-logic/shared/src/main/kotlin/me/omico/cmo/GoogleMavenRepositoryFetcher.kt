package me.omico.cmo

import org.gradle.api.resources.ResourceHandler

fun ResourceHandler.fetchGoogleMavenRepository(path: String): String =
    text.fromUri("https://maven.google.com/$path").asString()

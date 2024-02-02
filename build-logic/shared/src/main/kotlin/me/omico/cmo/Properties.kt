package me.omico.cmo

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> File.readProperties(): T =
    readLines()
        .associate { line ->
            val (key, value) = line.split("=")
            key to value
        }
        .let(Properties::decodeFromStringMap)

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> File.writeProperties(properties: T): Unit =
    Properties.encodeToStringMap(properties)
        .map { (key, value) -> "$key=$value" }
        .joinToString("\n")
        .let(::writeText)

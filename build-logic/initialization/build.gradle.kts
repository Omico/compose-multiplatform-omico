@file:Suppress("UnstableApiUsage")

plugins {
    `kotlin-dsl`
    embeddedKotlin("plugin.serialization")
}

dependencies {
    implementation(com.gradle.enterprise)
    implementation(gradmGeneratedJar)
}

dependencies {
    implementation("me.omico.cmo:cmo-shared")
}

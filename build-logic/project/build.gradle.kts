@file:Suppress("UnstableApiUsage")

plugins {
    `kotlin-dsl`
    embeddedKotlin("plugin.serialization")
}

dependencies {
    implementation(androidGradlePlugin)
    implementation(com.diffplug.spotless)
    implementation(gradmGeneratedJar)
    implementation(kotlinGradlePlugin)
    implementation(kotlinx.serialization.properties)
    implementation(me.omico.consensus.api)
    implementation(me.omico.consensus.dsl)
    implementation(me.omico.consensus.git)
    implementation(me.omico.consensus.publishing)
    implementation(me.omico.consensus.spotless)
}

dependencies {
    implementation("me.omico.cmo:cmo-shared")
}

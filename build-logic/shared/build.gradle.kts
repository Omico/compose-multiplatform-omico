@file:Suppress("UnstableApiUsage")

plugins {
    embeddedKotlin("jvm")
    embeddedKotlin("plugin.serialization")
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
}

dependencies {
    implementation(jsoup)
    implementation(kotlinx.serialization.properties)
    implementation(maven.core)
}

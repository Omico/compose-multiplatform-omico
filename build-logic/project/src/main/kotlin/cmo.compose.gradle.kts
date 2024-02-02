import me.omico.gradle.project.cmo.internal.configureCmoCompose
import me.omico.gradle.project.configureCommonAndroid

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("cmo.publishing")
}

kotlin {
    jvmToolchain(8)
}

configureCommonAndroid(
    domain = "me.omico.compose",
    compileSdk = 34,
    minSdk = 21,
    javaCompatibility = JavaVersion.VERSION_1_8,
)

configureCmoCompose()

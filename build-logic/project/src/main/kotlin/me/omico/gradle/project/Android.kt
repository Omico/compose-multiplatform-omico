package me.omico.gradle.project

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

internal fun Project.configureCommonAndroid(
    domain: String,
    compileSdk: Int,
    minSdk: Int,
    namespace: String = "$domain.${name.replace("-", ".")}",
    javaCompatibility: JavaVersion = JavaVersion.VERSION_11,
    isDesugaring: Boolean = true,
    coreLibraryDesugaringVersion: String = "2.0.4",
) {
    extensions.configure<CommonExtension<*, *, *, *, *>>("android") {
        this.namespace = namespace
        this.compileSdk = compileSdk
        defaultConfig {
            this.minSdk = minSdk
        }
        compileOptions {
            sourceCompatibility = javaCompatibility
            targetCompatibility = javaCompatibility
            if (isDesugaring) isCoreLibraryDesugaringEnabled = true
        }
    }
    if (isDesugaring) {
        dependencies.add("coreLibraryDesugaring", "com.android.tools:desugar_jdk_libs:$coreLibraryDesugaringVersion")
    }
}

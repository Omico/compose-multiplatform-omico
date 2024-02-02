import me.omico.consensus.dsl.by

plugins {
    id("me.omico.consensus.publishing")
}

consensus {
    publishing {
        publications.all {
            if (this !is MavenPublication) return@all
            pom {
                url by "https://github.com/Omico/compose-multiplatform-omico"
                licenses {
                    license {
                        name by "The Apache Software License, Version 2.0"
                        url by "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id by "Omico"
                        name by "Omico"
                    }
                }
                scm {
                    url by "https://github.com/Omico/compose-multiplatform-omico"
                    connection by "scm:git:https://github.com/Omico/compose-multiplatform-omico.git"
                    developerConnection by "scm:git:https://github.com/Omico/compose-multiplatform-omico.git"
                }
            }
        }
    }
}

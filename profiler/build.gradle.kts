import java.net.URI

plugins {
    id("java")
    id("maven-publish")
    id("signing")
}

dependencies {
    repositories {
        mavenCentral()
    }

    implementation("org.openjdk.jmh:jmh-core:1.37")
    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnit()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.fzhinkin"
            artifactId = "xctraceprof"
            version = "0.0.2-SNAPSHOT"

            from(components["java"])

            pom {
                name.set("XCTrace JMH profilers")
                description.set("Collection of JHM profilers using XCode Instruments as the underlying profiler.")
                url.set("https://github.com/fzhinkin/xctraceprof")
                licenses {
                    license {
                        name.set("GNU General Public License, version 2")
                        url.set("https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("fzhinkin")
                        name.set("Filipp Zhinkin")
                        email.set("filipp.zhinkin@gmail.com")
                    }
                }
                scm {
                    developerConnection.set("scm:git:git@github.com:fzhinkin/xctraceprof.git")
                    connection.set("scm:git:https://github.com/fzhinkin/xctraceprof.git")
                    url.set("https://github.com/fzhinkin/xctraceprof")
                }
            }
        }
    }

    repositories {
        maven {
            url = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrh.username") as String?
                password = project.findProperty("ossrh.password") as String?
            }
        }
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }

    publications.withType<MavenPublication>().all {
        if (project.findProperty("signing.keyId") != null) {
            project.extensions.configure<SigningExtension>("signing") {
                sign(this@all)
            }
        }
    }
}
//
//signing {
//    sign(publishing.publications["maven"])
//}


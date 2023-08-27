plugins {
    id("java")
    id("maven-publish")
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
}

tasks.test {
    useJUnit()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.fzhinkin"
            artifactId = "xctraceprof"
            version = "0.0.1"

            from(components["java"])

            pom {
                name.set("XCTrace JMH profilers")
                description.set("Collection of JHM profiling using XCode Instruments as the underlying profiler.")
                url.set("https://github.com/fzhinkin/XCTraceAsmProfiler")
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
                    developerConnection.set("scm:git:git@github.com:fzhinkin/XCTraceAsmProfiler.git")
                    connection.set("scm:git:https://github.com/fzhinkin/XCTraceAsmProfiler.git")
                    url.set("https://github.com/fzhinkin/XCTraceAsmProfiler")
                }
            }
        }
    }
}
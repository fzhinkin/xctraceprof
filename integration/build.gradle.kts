plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.1"
}

dependencies {
    repositories {
        mavenCentral()
    }

    implementation(project(":profiler"))
}

tasks {
    create("runBenchmark", JavaExec::class.java) {
        dependsOn.add("jmhJar")
        classpath += jmhJar.get().outputs.files
        mainClass = "io.github.fzhinkin.SampleBenchmark"
    }
}
import java.util.*

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
    create("runAsmProfWithDefaultArgs", JavaExec::class.java) {
        dependsOn.add("jmhJar")
        classpath += jmhJar.get().outputs.files
        mainClass = "io.github.fzhinkin.SampleBenchmark"
    }
    create("runAsmProfWithExplicitCpuProfiler", JavaExec::class.java) {
        dependsOn.add("jmhJar")
        classpath += jmhJar.get().outputs.files
        mainClass = "io.github.fzhinkin.SampleBenchmark"
        args("template=CPU Profiler")
    }
    create("runAsmProfWithExplicitTimeProfiler", JavaExec::class.java) {
        dependsOn.add("jmhJar")
        classpath += jmhJar.get().outputs.files
        mainClass = "io.github.fzhinkin.SampleBenchmark"
        args("template=Time Profiler")
    }
    check {
        dependsOn.addAll(Arrays.asList(
                "runAsmProfWithDefaultArgs",
                "runAsmProfWithExplicitCpuProfiler",
                "runAsmProfWithExplicitTimeProfiler"
        ))
    }
}
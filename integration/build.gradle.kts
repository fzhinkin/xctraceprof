import org.gradle.process.internal.ExecException
import java.io.ByteArrayOutputStream
import java.lang.IllegalStateException

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

val hasXCtrace: Boolean by lazy {
    try {
        exec {
            commandLine("xctrace", "version")
            isIgnoreExitValue = true
        }.exitValue == 0
    } catch (e: ExecException) {
        false
    }
}

tasks {
    create("runAsmProfWithDefaultArgs", JavaExec::class.java) {
        onlyIf { hasXCtrace && !project.hasProperty("noPMU") }
        dependsOn.add("jmhJar")

        classpath += jmhJar.get().outputs.files
        mainClass = "io.github.fzhinkin.XCTraceAsmProfilerTest"
    }
    create("runAsmProfWithExplicitCpuProfiler", JavaExec::class.java) {
        onlyIf { hasXCtrace && !project.hasProperty("noPMU") }
        dependsOn.add("jmhJar")
        classpath += jmhJar.get().outputs.files
        mainClass = "io.github.fzhinkin.XCTraceAsmProfilerTest"
        args("template=CPU Profiler")
    }
    create("runAsmProfWithExplicitTimeProfiler", JavaExec::class.java) {
        onlyIf { hasXCtrace }
        dependsOn.add("jmhJar")
        classpath += jmhJar.get().outputs.files
        mainClass = "io.github.fzhinkin.XCTraceAsmProfilerTest"
        args("template=Time Profiler")
    }
    create("runTimestampFiltrationTest", JavaExec::class.java) {
        onlyIf { hasXCtrace }
        dependsOn.add("jmhJar")
        classpath += jmhJar.get().outputs.files
        mainClass = "io.github.fzhinkin.TimestampFiltrationTest"
        val path = System.getenv("PATH")

        // use xctrace shell wrapper inserting 15 seconds delay before executing the record command
        val pathPrefix = fileTree(projectDir).matching {
            include("**/src/jmh/resources/xctrace")
        }.singleFile.parent
        environment("PATH", "$pathPrefix:$path")
    }
    create("checkAvailableProfilers", JavaExec::class.java) {
        dependsOn("jmhJar")
        mainClass = "-jar"
        args(jmhJar.get().outputs.files.singleFile.absolutePath, "-lprof")
        standardOutput = ByteArrayOutputStream()

        fun checkHasXCTraceAsmProfiler(seq: Sequence<String>, kind: String) {
            if (seq.count { it.contains("XCTraceAsmProfiler") } != 1) {
                throw IllegalStateException("XCTraceAsmProfiler not found among $kind profilers")
            }
        }

        fun checkHasXCTraceNormProfiler(seq: Sequence<String>, kind: String) {
            if (seq.count { it.contains("XCTraceNormProfiler") } != 1) {
                throw IllegalStateException("XCTraceNormProfiler not found among $kind profilers")
            }
        }

        doLast {
            val out = standardOutput.toString()
            val predicate: (String) -> Boolean = { !it.contains("Unsupported profilers") }
            val filter: (Sequence<String>) -> Sequence<String> = if (hasXCtrace) {
                { it.takeWhile(predicate) }
            } else {
                { it.dropWhile(predicate) }
            }
            val kind = if (hasXCtrace) "supported" else "unsupported"
            checkHasXCTraceAsmProfiler(filter(out.lineSequence()), kind)
            checkHasXCTraceNormProfiler(filter(out.lineSequence()), kind)
        }
    }
    check {
        dependsOn.addAll(listOf(
                "runAsmProfWithDefaultArgs",
                "runAsmProfWithExplicitCpuProfiler",
                "runAsmProfWithExplicitTimeProfiler",
                "runTimestampFiltrationTest",
                "checkAvailableProfilers"
        ))
    }
}
plugins {
    id("java")
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
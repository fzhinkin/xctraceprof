plugins {
    id("java")
}

dependencies {
    repositories {
        mavenCentral()
    }

    implementation("org.openjdk.jmh:jmh-core:1.37")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.test {
    useJUnitPlatform()
}
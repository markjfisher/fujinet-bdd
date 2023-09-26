plugins {
    kotlin("jvm") version "1.8.21"
    `maven-publish`
}

group = "fujinet"
version = "1.0.0-SNAPSHOT"

val assertJVersion: String by project
val mockkVersion: String by project
val junitJupiterEngineVersion: String by project

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = rootProject.name
            from(components["java"])
        }
    }
}

dependencies {
    implementation("BDD6502:BDD6502:1.0.9-SNAPSHOT")

    // fuck hamcrest. We're going jupiter/assertj baby.
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterEngineVersion")
    implementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterEngineVersion")
    implementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterEngineVersion")
    implementation("org.assertj:assertj-core:$assertJVersion")
    implementation("io.mockk:mockk:$mockkVersion")
    // testImplementation(kotlin("test"))
}

configurations {
    testCompileOnly {
        // GET THEE HENCE JUNIT4
        exclude(module = "junit", group = "junit")
    }
}

kotlin {
    jvmToolchain(11)
}

tasks {
    test {
        useJUnitPlatform()
    }
}

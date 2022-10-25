import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("com.github.johnrengelman.shadow") version("7.0.0")

    application
}

group = "at.lorenz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


tasks.withType<Jar> {
    manifest {
        attributes(mapOf(
            "Main-Class" to "at.lorenz.skyblockprogress.fetch.Main"
        ))
    }
}


dependencies {
    implementation("com.google.code.gson:gson:2.9.1")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}
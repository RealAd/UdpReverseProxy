import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}
group = "io.realad"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openlabtesting.netty:netty-all:4.1.48.Final")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    testImplementation(kotlin("test-junit5"))
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "io.realad.reverseproxy.udp.MainKt"
    }
}

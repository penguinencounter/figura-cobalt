plugins {
    id("java-library")
    id("maven-publish")
    id("cobalt-instrumentation") version "1.0-SNAPSHOT" // Include cobalt-instrumentation plugin
}

tasks.cobaltInstrumentation { dependsOn(*tasks.withType<JavaCompile>().toTypedArray()) }
tasks.jar { dependsOn(tasks.cobaltInstrumentation) }

group = "org.figuramc"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Cobalt dependencies
    compileOnly("org.checkerframework:checker-qual:3.36.0")
    compileOnly("org.jetbrains:annotations:24.1.0")

    // Expose build tools as implementation
    implementation(project(":cobalt-build-tools"))

    // Figura
    implementation("org.figuramc:memory-tracker:1.0-SNAPSHOT")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}



java {
    withSourcesJar()
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}
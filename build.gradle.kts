plugins {
    id("java-library")
    id("maven-publish")
}

group = "org.figuramc"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

// Cobalt instrumentation
val cobaltBuildTools by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val luaDirectory = project.layout.buildDirectory.dir("classes/java/main/org/figuramc/figura_cobalt").get()
val instrumentForCobalt = tasks.register("InstrumentForCobalt", JavaExec::class) {
    dependsOn(tasks.compileJava)

    inputs.dir(luaDirectory).withPropertyName("inputDir")
    outputs.dir(luaDirectory).withPropertyName("outputDir")

    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    mainClass = "cc.tweaked.cobalt.build.MainKt"
    classpath = cobaltBuildTools

    args = listOf(luaDirectory.asFile.absolutePath)
}
tasks.jar { dependsOn(instrumentForCobalt) }

dependencies {
    // Cobalt dependencies
    compileOnly("org.checkerframework:checker-qual:3.36.0")
    compileOnly("org.jetbrains:annotations:24.1.0")

    // Cobalt build tools
    project(":cobalt-build-tools")
    cobaltBuildTools(project(":cobalt-build-tools"))

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
plugins {
	kotlin("jvm") version "2.2.10"
	`kotlin-dsl`
	`maven-publish`
}

group = "org.figuramc"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {

	implementation("org.slf4j:slf4j-api:2.0.6")

	implementation("org.ow2.asm:asm:9.6")
	implementation("org.ow2.asm:asm-analysis:9.6")
	implementation("org.ow2.asm:asm-commons:9.6")
	implementation("org.ow2.asm:asm-tree:9.6")
	implementation("org.ow2.asm:asm-util:9.6")

	implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

	runtimeOnly("org.slf4j:slf4j-simple:2.0.6")
}

gradlePlugin {
	plugins.register("cobalt-instrumentation") {
		id = "cobalt-instrumentation"
		implementationClass = "org.figuramc.figura_cobalt.transformer.CobaltInstrumentationPlugin"
	}
}

publishing {
	repositories {
		mavenLocal()
	}
	publications.create<MavenPublication>("maven") {
		from(components["java"])
	}
}
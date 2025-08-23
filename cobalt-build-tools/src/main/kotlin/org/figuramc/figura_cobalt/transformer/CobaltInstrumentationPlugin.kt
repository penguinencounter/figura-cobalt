package org.figuramc.figura_cobalt.transformer

import org.figuramc.figura_cobalt.transformer.cc.tweaked.cobalt.build.instrument
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.invariantSeparatorsPathString

private val SUFFIX = "__COBALT_INSTRUMENTATION_TEMP"

class CobaltInstrumentationPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register<CobaltInstrumentationTask>("cobaltInstrumentation")

        project.tasks.withType<JavaCompile>().forEach {
            val original = it.destinationDirectory.get().asFile.toPath().invariantSeparatorsPathString
            val extended = original + SUFFIX
            val extendedFile = Path(extended).toFile()
            it.destinationDirectory.set(extendedFile)
        }
    }

    open class CobaltInstrumentationTask: DefaultTask() {
        @TaskAction
        fun instrument() {
            project.extensions.findByType<SourceSetContainer>()!!.forEach { sourceSet ->
                val output = sourceSet.output.classesDirs.asPath
                val input = output + SUFFIX
                println("Processing source set \"${sourceSet.name}\" from \"${input}\"")
                instrument(Path(input).absolute(), Path(output).absolute())
            }
        }
    }

}
package org.figuramc.figura_cobalt.transformer.cc.tweaked.cobalt.build

import org.figuramc.figura_cobalt.transformer.cc.tweaked.cobalt.build.coroutine.CoroutineInstrumentation
import org.figuramc.figura_cobalt.transformer.cc.tweaked.cobalt.build.coroutine.DefinitionScanner
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.extension

class UnsupportedConstruct(message: String, cause: Exception? = null) : RuntimeException(message, cause)

fun instrument(input: Path, output: Path) {

	if (!input.exists()) return

	println("Transforming from $input to $output")

	val definitions = DefinitionScanner()
	val instrumentedClasses = mutableListOf<ClassReader>()
	Files.find(input, Int.MAX_VALUE, { path, _ -> path.extension == "class" }).use { files ->
		files.forEach { inputFile ->
			val reader = Files.newInputStream(inputFile).use { ClassReader(it) }
			if (definitions.addClass(reader)) {
				// If this class will be transformed, add it to the instrumented list
				println("Will instrument $inputFile")
				instrumentedClasses.add(reader)
			} else {
				// Otherwise copy it directly
				val outputFile = output.resolve(input.relativize(inputFile))
				Files.createDirectories(outputFile.parent)
				Files.copy(inputFile, outputFile, StandardCopyOption.REPLACE_EXISTING)
			}
		}
	}

	val emitter = FileClassEmitter(output)
	for (klass in instrumentedClasses) {
		emitter.generate(klass.className, klass, ClassWriter.COMPUTE_FRAMES) { cw ->
			klass.accept(CoroutineInstrumentation(Opcodes.ASM9, cw, definitions, emitter), ClassReader.EXPAND_FRAMES)
		}
	}
}

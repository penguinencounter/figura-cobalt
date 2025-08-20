package cc.tweaked.cobalt.build

import cc.tweaked.cobalt.build.coroutine.CoroutineInstrumentation
import cc.tweaked.cobalt.build.coroutine.DefinitionScanner
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.extension

class UnsupportedConstruct(message: String, cause: Exception? = null) : RuntimeException(message, cause)

fun main(args: Array<String>) {
//	if (args.size != 2) {
//		System.err.println("Expected: INPUT OUTPUT")
//		exitProcess(1)
//	}

	val directory = Paths.get(args[0])
	println("Directory: $directory")
	var tempDir = directory
	var classPrefix: String
	do {
		classPrefix = tempDir.relativize(directory).toString().replace(File.separatorChar, '/') + "/"
		tempDir = tempDir.parent
	} while (!tempDir.endsWith("java"))

	println("Class prefix: $classPrefix")
	tempDir = tempDir.resolve("FIGURA_COBALT_temp")
	println("Temp directory: $tempDir")


	val definitions = DefinitionScanner()
	val instrumentedClasses = mutableListOf<ClassReader>()
	Files.find(directory, Int.MAX_VALUE, { path, _ -> path.extension == "class" }).use { files ->
		files.forEach { inputFile ->
			val reader = Files.newInputStream(inputFile).use { ClassReader(it) }
			if (definitions.addClass(reader)) {
				val tempFile = tempDir.resolve(directory.relativize(inputFile));
				println(tempFile)
				Files.createDirectories(tempFile.parent)
				Files.copy(inputFile, tempFile, StandardCopyOption.REPLACE_EXISTING)
				instrumentedClasses.add(Files.newInputStream(tempFile).use { ClassReader(it) })
			}
		}
	}

	val emitter = FileClassEmitter(classPrefix, directory)
	for (klass in instrumentedClasses) {
		emitter.generate(klass.className, klass, ClassWriter.COMPUTE_FRAMES) { cw ->
			klass.accept(CoroutineInstrumentation(Opcodes.ASM9, cw, definitions, emitter), ClassReader.EXPAND_FRAMES)
		}
	}
}

package common

import eotree.EOProgram
import lexer.Scanner
import org.junit.jupiter.api.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import parser.JavaParser
import translator.Translator
import tree.Compilation.CompilationUnit
import util.logger
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.Locale

@Execution(ExecutionMode.CONCURRENT)
// @Execution(ExecutionMode.SAME_THREAD)
class TestJ2EO {
    @TestFactory
    fun testChapter4(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_4_types_values_variables")
    }

    @TestFactory
    fun testChapter5(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_5_conversions_and_contexts")
    }

    @TestFactory
    fun testChapter6(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_6_names")
    }

    @TestFactory
    fun testChapter7(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_7_packages_and_modules")
    }

    @TestFactory
    fun testChapter8(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_8_classes")
    }

    @TestFactory
    fun testChapter9(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_9_interfaces")
    }

    @TestFactory
    fun testChapter10(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_10_arrays")
    }

    @TestFactory
    fun testChapter11(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_11_exceptions")
    }

    @TestFactory
    fun testChapter14(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_14_blocks_statements_and_patterns")
    }

    @TestFactory
    fun testChapter15(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_15_expressions")
    }

    @TestFactory
    fun testChapter16(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_16_definite_assignment")
    }

    @TestFactory
    fun testChapter17(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_17_threads_and_locks")
    }

    @TestFactory
    fun testChapter18(): Collection<DynamicTest> {
        return testChapter(testFolderRoot + sep + "ch_18_type_inference")
    }

    @TestFactory
    fun simpleTest(): Collection<DynamicTest> =
        listOf(
            testFile(Paths.get(testFolderRoot + sep + "SimpleTest.java"))
        )

    private fun testChapter(chapterPath: String): List<DynamicTest> {
        return File(chapterPath).walk()
            .filter { file -> file.isFile }
            .filter { file -> isReadyTest(file.toPath()) }
            .filter { file -> isNotClassFile(file.toPath()) }
            .filter { file -> isJavaFile(file.toPath()) }
            .map { file -> testFile(file.toPath()) }
            .toList()
    }

    companion object {
        private var testFolderRoot: String? = null
        private var pomFilePath: String? = null
        private var mainFolderRoot: String? = null
        private val sep = File.separatorChar.toString()

        @BeforeAll
        @JvmStatic
        fun setup() {
            val testCandidates =
                System.getProperty("candidates") != null &&
                    System.getProperty("candidates") == "true"
            if (testCandidates)
                logger.info("-- Executing candidate tests --")
            var testFolderPath = listOf("src", "test", "resources").joinToString(sep)
            testFolderPath += sep + if (testCandidates) "test_candidates" else "test_ready"
            val mainFolderPath = listOf("src", "main", "resources").joinToString(sep)
            val fileMain = File(mainFolderPath)
            mainFolderRoot = fileMain.absolutePath
            val fileTest = File(testFolderPath)
            testFolderRoot = fileTest.absolutePath

            val pomPath = listOf("src", "test", "resources", "eo_execution_pom", "pom.xml").joinToString(sep)
            pomFilePath = File(pomPath).absolutePath
        }

        private fun testFile(path: Path): DynamicTest {
            return DynamicTest.dynamicTest(
                path.parent.fileName.toString() + "/" +
                    path.fileName.toString()
            ) {
                assertTimeoutPreemptively(
                    Duration.ofMinutes(10)
                ) {
                    logger.info("-- Current test file: $path")

                    // Compile and execute Java file
                    // FIXME: EO should generate '\r\n' symbols for Windows
                    val javaExecOutput = compileAndExecuteJava(path).replace("\r\n", "\n")

                    // Run parser
                    val unit = parseAndBuildAST(path)

                    // EO tree to string
                    val eoCode = translateToEO(unit)
                    logger.info("-- Translation output --" + System.lineSeparator() + eoCode)

                    // Compile and execute translated to EO Java file
                    val eoExecOutput = compileAndExecuteEO(eoCode, path)

                    // Assert equal execution outputs
                    Assertions.assertEquals(javaExecOutput, eoExecOutput)
                }
            }
        }

        private fun compileAndExecuteJava(path: Path): String {
            val fileName = path.fileName.toString().split(".").toTypedArray()[0]
            val subFolder: File = Path.of(path.parent.toString(), "${fileName}_java").toFile()
            if (subFolder.exists()) {
                subFolder.deleteRecursively()
            }

            try {
                val output = StringBuilder()
                // Compile .java file
                Files.createDirectories(subFolder.toPath())
                Files.copy(path, Paths.get(subFolder.toString(), path.fileName.toString()))
                val compilePb = ProcessBuilder(
                    "javac",
                    "-d",
                    subFolder.toString(),
                    path.toString()
                )
                compilePb.directory(File(subFolder.toString()))
                compilePb.redirectErrorStream(true)
                val compileProcess = compilePb.start()
                val stdCompInput = BufferedReader(InputStreamReader(compileProcess.inputStream))
                var sc: String?
                val javacSb = StringBuilder()
                while (stdCompInput.readLine().also { sc = it } != null) {
                    javacSb.append(sc).append(System.lineSeparator()) // Java compilation output (if any)
                }
                compileProcess.waitFor()
                compileProcess.destroy()
                logger.info("-- Java compilation output --" + System.lineSeparator() + javacSb.toString())

                // Execute .class file
                val execPb = ProcessBuilder(
                    "java",
                    "-cp",
                    subFolder.toString() + sep,
                    fileName
                )
                execPb.directory(File(subFolder.toString()))
                execPb.redirectErrorStream(true)
                val execProcess = execPb.start()

                // Receive output
                val stdInput = BufferedReader(InputStreamReader(execProcess.inputStream))
                var s: String?
                while (stdInput.readLine().also { s = it } != null) {
                    output.append(s).append(System.lineSeparator())
                }
                execProcess.waitFor()
                execProcess.destroy()
                logger.info("-- Java execution output --" + System.lineSeparator() + output.toString())

                // Remove .class files
                subFolder.deleteRecursively()
                return output.toString()
            } catch (e: Exception) {
                // Cleanup output and rethrow exception
                subFolder.deleteRecursively()
                throw e
            }
        }

        private fun parseAndBuildAST(path: Path): CompilationUnit {
            val scanner = Scanner()
            scanner.readFile(path.toString())
            val parser = JavaParser(scanner)
            if (!parser.parse()) {
                logger.error("Unable to parse a .java file: \"$path\"")
                throw IllegalStateException("Parsing Java file \"$path\" failed")
            }
            return parser.ast
        }

        private fun translateToEO(unit: CompilationUnit): String {
            val eoProgram: EOProgram = Translator().translate(unit)
            return eoProgram.generateEO(0)
        }

        /**
         * The process is following:<br></br>
         * 1. In the same parent folder as the ".java" test file has create subfolder.<br></br>
         * 2. In that subfolder create "eo" subfolder.<br></br>
         * 3. In "eo" subfolder create "main.eo" file and write previously generated EO code to it.<br></br>
         * 4. Alongside "eo" subfolder make a copy of "pom.xml" file fo Maven.<br></br>
         * 5. Compile "main.eo" with <pre>mvn clean compile</pre>
         * 6. Execute with <pre>java -cp ...</pre>
         * 7. Receive execution output<br></br>
         * 8. Pass received output back<br></br>
         * @param eoCode previously generated EO code
         * @param testFilePath path to the ".java" test file
         * @return EO execution output
         */
        private fun compileAndExecuteEO(eoCode: String?, testFilePath: Path): String {
            val eoFileName = testFilePath.fileName.toString().split(".").toTypedArray()[0]
            val testFolder: File = Path.of(testFilePath.parent.toString(), "${eoFileName}_eo").toFile()
            // Setup temporary folders and files
            if (testFolder.exists()) {
                testFolder.deleteRecursively()
            }
            val eoExecDir = Files.createDirectories(Paths.get(testFolder.toString(), "eo"))

            try {
                val eoFilePath = Files.createFile(Paths.get(eoExecDir.toString() + sep + "class_" + eoFileName + ".eo"))
                Files.copy(
                    Paths.get(pomFilePath ?: "pomPath"),
                    Paths.get(eoExecDir.parent.toString() + sep + "pom.xml")
                )
                Paths.get(mainFolderRoot ?: "mainPath", "stdlib").toFile().copyRecursively(
                    Paths.get(eoExecDir.toString(), "stdlib").toFile()
                )

                // Write generated code to the file
                Files.writeString(eoFilePath, eoCode)

                // Execute generated EO code
                val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault())
                    .contains("windows") // Matters a lot

                // Compile EO file
                val mvnCommands = if (isWindows) listOf("mvn.cmd", "clean", "compile")
                else listOf("mvn", "clean", "compile")
                val compileProcess = ProcessBuilder(mvnCommands)
                    .directory(File(eoExecDir.parent.toString()))
                    .redirectErrorStream(true)
                    .start()

                // Receive compilation output (may be useful)
                val mvnStdInput = BufferedReader(InputStreamReader(compileProcess.inputStream))
                var m: String?
                val mvnSb = StringBuilder()
                while (mvnStdInput.readLine().also { m = it } != null) {
                    mvnSb.append(m).append(System.lineSeparator())
                }
                compileProcess.waitFor()
                compileProcess.destroy()
                logger.info(" -- EO compilation output --" + System.lineSeparator() + mvnSb.toString())

                // Execute Java ".class"es
                val execPb = ProcessBuilder(
                    "java", "-cp",
                    if (isWindows)
                        "\"target/classes;target/eo-runtime.jar\""
                    else
                        "target/classes:target/eo-runtime.jar",
                    "org.eolang.Main",
                    "main",
                    if (isWindows)
                        "%*"
                    else
                        "\"$@\"1"
                )
                execPb.directory(File(eoExecDir.parent.toString()))
                execPb.redirectErrorStream(true)
                val execProcess = execPb.start()

                // Receive EO execution output
                val stdInput = BufferedReader(InputStreamReader(execProcess.inputStream))
                val eoExecOut: String = stdInput.readText()
                execProcess.waitFor()
                execProcess.destroy()
                logger.info("-- EO execution output --\n$eoExecOut")

                // Clean everything out
                eoExecDir.toAbsolutePath().parent.toFile().deleteRecursively()

                // Double check for delete, so that no obsolete files are created after test fail
                if (testFolder.exists()) {
                    try {
                        testFolder.deleteRecursively()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                return eoExecOut
            } catch (e: Exception) {
                // Clean everything out
                eoExecDir.toAbsolutePath().parent.toFile().deleteRecursively()

                // Double check for delete, so that no obsolete files are created after test fail
                if (testFolder.exists())
                    testFolder.deleteRecursively()
                throw e
            }
        }

        private fun isReadyTest(path: Path): Boolean {
            return !path.endsWith("SampleTest.java")
        }

        private fun isClassFile(path: Path): Boolean {
            return path.toString().endsWith(".class")
        }

        private fun isJavaFile(path: Path): Boolean {
            return path.toString().endsWith(".java")
        }

        private fun isNotClassFile(path: Path): Boolean {
            return !isClassFile(path)
        }
    }
}

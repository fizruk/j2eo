package translator

import arrow.core.None
import arrow.core.Some
import eotree.*
import translator.preprocessor.PreprocessorState
import translator.preprocessor.preprocess
import tree.Compilation.CompilationUnit
import tree.Compilation.Package
import tree.Compilation.SimpleCompilationUnit
import tree.Compilation.TopLevelComponent
import tree.Entity
import util.findMainClass
import util.generateEntryPoint
import util.logger
import java.time.LocalDateTime
import java.util.*
import kotlin.streams.toList

class Translator {

    fun translate(unit: CompilationUnit): EOProgram {
        return if (unit is SimpleCompilationUnit)
            mapSimpleCompilationUnit(unit)
        else
            throw IllegalArgumentException(
                "CompilationUnit of type " +
                        unit.javaClass.simpleName +
                        " is not supported"
            )
    }

    fun mapPackage(pkg: Package): EOProgram {
        return EOProgram(
            EOLicense(), // TODO: add license?
            EOMetas(
                Some(pkg.compoundName.names.joinToString(".")),
                ArrayList()
            ),
            pkg.components.components
                .map { obj -> mapTopLevelComponent(obj) }
        )
    }

    private fun mapSimpleCompilationUnit(unit: SimpleCompilationUnit): EOProgram {
        // preprocessUnit(unit)
        val preprocessorState = PreprocessorState()
        preprocess(preprocessorState, unit)

        val bnds = unit.components.components
            .map { obj: TopLevelComponent? -> mapTopLevelComponent(obj!!) }
            .map { bnd: EOBnd -> bnd as EOBndExpr }


        // FIXME: assuming there is only one top-level component and it is a class
        var entrypointBnds = listOf<EOBndExpr>()
        try {
            val mainClassName = findMainClass(unit)
            entrypointBnds = generateEntryPoint(mainClassName)
        } catch (e: NullPointerException) {
            logger.info { "No entry point here!" }
        }

        // FIXME: assuming there is only one top-level component and it is a class
        // Always calling the 'main' method

        val stdAliases = preprocessorState.stdClassesForCurrentAlias.stream()
                .map { EOMeta("alias", "stdlib.$it") }.toList()
        val eoAliases = preprocessorState.eoClassesForCurrentAlias.stream()
                .map { EOMeta("alias", "org.eolang.$it") }.toList()

        return EOProgram(
            EOLicense(
                EOComment(LocalDateTime.now().toString()),
                EOComment("j2eo team")
            ),
            EOMetas(
                None,
                stdAliases + eoAliases
            ),
            bnds + entrypointBnds
        )
    }

    fun mapTopLevelComponent(component: TopLevelComponent): EOBnd {
        return if (component.classDecl != null) {
            mapClass(component.classDecl)
        } else if (component.interfaceDecl != null) {
            mapInterface(component.interfaceDecl)
        } else {
            throw IllegalArgumentException("Supplied TopLevelComponent does not have neither class nor interface")
        }
    }
}

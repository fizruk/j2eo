package util

import lexer.TokenCode
import tree.Compilation.SimpleCompilationUnit
import tree.Compilation.TopLevelComponent
import tree.Declaration.Declaration
import tree.Declaration.MethodDeclaration
import tree.Declaration.NormalClassDeclaration

fun containsMain(component: TopLevelComponent): Boolean {
    return if (component.classDecl != null) {
        try {
            (component.classDecl as NormalClassDeclaration).body.declarations
                .filterIsInstance<MethodDeclaration>()
                .find { declaration: Declaration? ->
                    try {
                        (declaration as MethodDeclaration).parameters.parameters.size == 1 &&
                            declaration.parameters.parameters[0].name == "args" &&
                            declaration.name == "main" &&
                            declaration.modifiers.modifiers.modifiers.size == 2 &&
                            declaration.modifiers.modifiers.modifiers.find { it == TokenCode.Static } != null &&
                            declaration.modifiers.modifiers.modifiers.find { it == TokenCode.Public } != null
                    } catch (e: NullPointerException) {
                        false
                    }
                } != null
        } catch (e: NullPointerException) {
            false
        }
    } else {
        false
    }
}

fun findMainClass(unit: SimpleCompilationUnit): String {
    return unit.components.components.find { component: TopLevelComponent -> containsMain(component) }!!.classDecl.name
}

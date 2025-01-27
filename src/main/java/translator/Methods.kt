package translator

import arrow.core.*
import eotree.EOBndExpr
import eotree.EOCopy
import eotree.EOObject
import eotree.eoDot
import lexer.TokenCode
import tree.Declaration.MethodDeclaration
import tree.Declaration.ParameterDeclaration
import tree.Declaration.VariableDeclaration
import tree.Entity
import tree.Expression.Expression
import tree.Initializer
import tree.Statement.BlockStatements
import util.ParseExprTasks
import kotlin.collections.flatten

// fun MethodDeclaration.getLocalVariables(): List<Declaration> =
//    // TODO: add support for nested block variables as well
//    methodBody.block.blockStatements
//        .filter { it.declaration != null }
//        .map {  }

fun mapMethodDeclaration(dec: MethodDeclaration): EOBndExpr {
    val isStatic = dec.modifiers != null &&
            dec.modifiers.modifiers.modifiers.find { it == TokenCode.Static } != null
    val additionalParameters = if (!isStatic) listOf("this") else ArrayList()
    val parseExprTasks = ParseExprTasks()

    val obj = EOObject(
        // Non-vararg parameters
        if (dec.parameters != null) // Exclude 'String[] args' fon now
            additionalParameters +
                    dec.parameters.parameters
                        .filter { param: ParameterDeclaration -> !param.signEllipsis }
                        .map { param: ParameterDeclaration -> param.name }
                        .toList()
        else
            additionalParameters,

        // Optional vararg parameter
        if (dec.parameters != null)
            dec.parameters.parameters
                .filter { param: ParameterDeclaration -> param.signEllipsis }
                .map { param: ParameterDeclaration -> param.name }
                .also { assert(it.size <= 1) }
                .firstOrNone()
        else
            None,

        // Bound attributes
        // TODO: implement
//        try {
        if (dec.methodBody != null) (
        dec.methodBody.block.findAllLocalVariables().map { preMapVariableDeclaration(it) } +
                listOf(
                    EOBndExpr(
                        EOCopy(
                            "seq",
                            dec.methodBody.block.blockStatements
                                .mapNotNull {
                                    if (it.statement != null)
                                        mapStatement(parseExprTasks, it.statement)
                                    else if (it.expression != null) {
                                        parseExprTasks.addTask(it.expression).eoDot()
                                    } else if (it.declaration is VariableDeclaration && (it.declaration as VariableDeclaration).initializer != null)
                                        mapVariableDeclaration(parseExprTasks, it.declaration as VariableDeclaration)
                                    else
                                        null
                                }
                        ),
                        "@"
                    )
                ) + parseExprTasks(parseExprTasks)) else
//        } catch (e: NullPointerException) {
            listOf(
                EOBndExpr(
                    EOCopy(
                        "0",
                        ArrayList()
                    ),
                    "@"
                )
            ),
//        },

        "${dec.name} :: ${
            dec.parameters?.parameters
                ?.joinToString(" -> ") { param ->
                    param.type.getTypeName() + param.signEllipsis.let { if (it) "..." else "" }
                }
        } -> ${
            Option.fromNullable(dec.type)
                .map { it.getTypeName() }
                .getOrElse { "void" }
        }"
    )

    // Contract to check parameter count
    if (dec.parameters != null)
        assert(
            dec.parameters.parameters.size + (if (isStatic) 0 else 1) ==
                    obj.freeAttrs.size + if (obj.varargAttr.nonEmpty()) 1 else 0
        ) {
            "Parameters count of Java method and EO method do not match: ${dec.parameters.parameters.size} vs ${obj.freeAttrs.size + if (obj.varargAttr.nonEmpty()) 1 else 0}"
        }

    return EOBndExpr(
        obj,
        dec.name
    )
}

fun parseExprTasks(parseExprTasks: ParseExprTasks): List<EOBndExpr> {
    return if (parseExprTasks.tasks.size > 0) {
        parseExprTasks.tasks
            .map { parseExprTask(it.second, it.first) }
            .flatten()
    } else {
        listOf()
    }
}

fun parseExprTask(e: Entity, name: String): List<EOBndExpr> {
    val parseExprTasks = ParseExprTasks()
    return when (e) {
        is Expression -> {
            listOf(EOBndExpr(mapExpression(parseExprTasks, e), name)) + parseExprTasks(parseExprTasks)
        }
        is Initializer -> {
            listOf(EOBndExpr(mapInitializer(parseExprTasks, e), name)) + parseExprTasks(parseExprTasks)
        }
        else -> {
            throw IllegalArgumentException("Entity of type ${e.javaClass.simpleName} cannot be parsed")
        }
    }
}

fun BlockStatements.findAllLocalVariables(): List<VariableDeclaration> =
    blockStatements
        .filter { it.declaration is VariableDeclaration }
        .map { it.declaration as VariableDeclaration }

package io.gitlab.arturbosch.detekt.cli.runners

import io.gitlab.arturbosch.detekt.api.DetektVisitor
import io.gitlab.arturbosch.detekt.cli.CliArgs
import io.gitlab.arturbosch.detekt.core.KtCompiler
import io.gitlab.arturbosch.detekt.core.isFile
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.psi.KtContainerNode
import org.jetbrains.kotlin.psi.KtDeclarationContainer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStatementExpression

class AstPrinter(private val arguments: CliArgs) : Executable {

    override fun execute() {
        val inputPaths = arguments.inputPaths
        val inputPathsSize = inputPaths.size

        require(inputPathsSize == 1) {
            "$inputPathsSize input paths specified. Printing AST is only supported for single files."
        }

        val input = inputPaths.first()

        require(input.isFile()) {
            "Input path ($input) must be a kotlin file and not a directory."
        }

        val ktFile = KtCompiler().compile(input, input)
        println(ElementPrinter.dump(ktFile))
    }
}

class ElementPrinter : DetektVisitor() {

    companion object {
        fun dump(file: KtFile): String = ElementPrinter().run {
            sb.appendln("0: " + file.javaClass.simpleName)
            visitKtFile(file)
            sb.toString()
        }
    }

    private val sb = StringBuilder()

    private val indentation
        get() = (0..indent).joinToString("") { "  " }

    private val KtElement.line
        get() = PsiDiagnosticUtils.offsetToLineAndColumn(
            containingFile.viewProvider.document,
            textRange.startOffset
        ).line

    private val KtElement.dump
        get() = indentation + line + ": " + javaClass.simpleName

    private var indent: Int = 0
    private var lastLine = 0

    override fun visitKtElement(element: KtElement) {
        val currentLine = element.line
        if (element.isContainer()) {
            indent++
            sb.appendln(element.dump)
        } else {
            if (lastLine == currentLine) {
                indent++
                sb.appendln(element.dump)
                indent--
            } else {
                sb.appendln(element.dump)
            }
        }
        lastLine = currentLine
        super.visitKtElement(element)
        if (element.isContainer()) {
            indent--
        }
    }

    private fun KtElement.isContainer() =
        this is KtStatementExpression ||
                this is KtDeclarationContainer ||
                this is KtContainerNode
}

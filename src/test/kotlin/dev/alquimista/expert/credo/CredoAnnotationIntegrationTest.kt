package dev.alquimista.expert.credo

import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl
import com.intellij.lang.annotation.AnnotationSession
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.mock.MockProject
import com.intellij.testFramework.ParsingTestCase
import dev.alquimista.expert.lang.ElixirParserDefinition
import java.nio.file.Paths

@Suppress("DEPRECATION")
class CredoAnnotationIntegrationTest : ParsingTestCase("", "ex", ElixirParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testFindingBecomesWarningAtItsSourceLocation() {
        val settings = CredoSettings().apply { state.enabled = true }
        (project as MockProject).registerService(CredoSettings::class.java, settings)
        val text = "value = IO.inspect(1)\n"
        val file = parseFile("sample.ex", text)
        val root = Paths.get("/project")
        val input = CredoInput(project, root, root, root.resolve("sample.ex"), "sdk", text)
        val issue = CredoIssue("sample.ex", 1, 9, 11, "Avoid inspect", "warning", "Credo.Check.Warning.IoInspect")
        val holder = AnnotationHolderImpl(AnnotationSession(file), false)
        holder.runAnnotatorWithContext(file, Annotator { _, annotations ->
            CredoExternalAnnotator().apply(file, CredoResult(input, listOf(issue)), annotations)
        })
        assertEquals(1, holder.size)
        assertEquals(HighlightSeverity.WARNING, holder[0].severity)
        assertEquals("IO", text.substring(holder[0].startOffset, holder[0].endOffset))
        assertTrue(holder[0].message!!.contains("Avoid inspect"))
    }

    fun testOutdatedOrDisabledFindingsAreNotApplied() {
        val settings = CredoSettings().apply { state.enabled = true }
        (project as MockProject).registerService(CredoSettings::class.java, settings)
        val file = parseFile("sample.ex", "new_content\n")
        val root = Paths.get("/project")
        val input = CredoInput(project, root, root, root.resolve("sample.ex"), "sdk", "old_content\n")
        val issue = CredoIssue("sample.ex", 1, 1, 2, "Old finding", "warning", "")
        val holder = AnnotationHolderImpl(AnnotationSession(file), false)
        holder.runAnnotatorWithContext(file, Annotator { _, annotations ->
            CredoExternalAnnotator().apply(file, CredoResult(input, listOf(issue)), annotations)
        })
        assertTrue(holder.isEmpty())
        settings.state.enabled = false
        val current = input.copy(text = file.text)
        holder.runAnnotatorWithContext(file, Annotator { _, annotations ->
            CredoExternalAnnotator().apply(file, CredoResult(current, listOf(issue)), annotations)
        })
        assertTrue(holder.isEmpty())
    }
}

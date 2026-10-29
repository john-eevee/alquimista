package ex.alquimista.expert.lsp

import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.ParsingTestCase
import com.intellij.mock.MockProject
import com.intellij.openapi.project.BaseProjectDirectories
import com.intellij.openapi.vfs.VirtualFile
import ex.alquimista.expert.lang.ElixirParserDefinition

class ExpertLanguageIdTest : ParsingTestCase("", "ex", ElixirParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testDocumentLanguageIdsMatchExpert() {
        (project as MockProject).registerService(BaseProjectDirectories::class.java, object : BaseProjectDirectories(project) {
            override val isProcessing: Boolean = false
            override fun getBaseDirectories(): Set<VirtualFile> = emptySet()
            override fun getBaseDirectoryFor(virtualFile: VirtualFile): VirtualFile? = null
        })
        val descriptor = ExpertLspServerDescriptor(project)
        for ((name, expected) in mapOf(
            "module.ex" to "elixir", "script.exs" to "elixir", "page.eex" to "eex", "page.heex" to "heex"
        )) {
            assertEquals(name, expected, descriptor.getLanguageId(LightVirtualFile(name, "")))
        }
    }
}

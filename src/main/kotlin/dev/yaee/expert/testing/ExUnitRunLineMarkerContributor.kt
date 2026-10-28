package dev.yaee.expert.testing

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import dev.yaee.expert.lang.ElixirLineToken
import java.nio.file.Paths

internal class ExUnitRunLineMarkerContributor : RunLineMarkerContributor(), DumbAware {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != ElixirLineToken) return null
        val project = element.project
        val projectRoot = project.basePath ?: return null
        val virtualFile = element.containingFile.virtualFile ?: return null
        if (!ExUnitTestDetector.isTestFile(virtualFile.name)) return null
        val path = runCatching {
            Paths.get(projectRoot).toAbsolutePath().normalize()
                .relativize(Paths.get(virtualFile.path).toAbsolutePath().normalize())
        }.getOrNull() ?: return null
        if (path.isAbsolute || path.startsWith("..")) return null

        val source = element.containingFile.text
        val line = source.substring(0, element.textRange.startOffset).count { it == '\n' } + 1
        val detected = ExUnitTestDetector.detect(source).firstOrNull { it.line == line } ?: return null
        val relativePath = path.toString().replace('\\', '/')
        val action = RunExUnitTestAction(relativePath, line, detected.displayName)
        return Info(AllIcons.RunConfigurations.TestState.Run, arrayOf(action)) {
            "Run ${detected.displayName}"
        }
    }
}

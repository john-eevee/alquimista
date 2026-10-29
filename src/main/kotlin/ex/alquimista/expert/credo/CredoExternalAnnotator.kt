package ex.alquimista.expert.credo

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import java.nio.file.Path
import java.nio.file.Paths

data class CredoInput(
    val project: Project,
    val root: Path,
    val mixRoot: Path,
    val file: Path,
    val sdkKey: String,
    val text: String
)

data class CredoResult(val input: CredoInput, val issues: List<CredoIssue>)

class CredoExternalAnnotator : ExternalAnnotator<CredoInput, CredoResult>() {
    override fun collectInformation(file: PsiFile): CredoInput? {
        val project = file.project
        if (!CredoSettings.getInstance(project).state.enabled) return null
        project.getService(CredoRunner::class.java)
        val virtualFile = file.virtualFile ?: return null
        if (virtualFile.extension !in setOf("ex", "exs")) return null
        val root = project.basePath?.let(Paths::get)?.toAbsolutePath()?.normalize() ?: return null
        val path = Paths.get(virtualFile.path).toAbsolutePath().normalize()
        val mixRoot = CredoProject.findMixRoot(root, path) ?: return null
        val sdk = ProjectRootManager.getInstance(project).projectSdk
        val sdkKey = "${sdk?.sdkType?.name}:${sdk?.homePath}"
        return CredoInput(project, root, mixRoot, path, sdkKey, file.text)
    }

    override fun doAnnotate(input: CredoInput): CredoResult? {
        val issues = input.project.getService(CredoRunner::class.java)
            .analyze(input.root, input.mixRoot, input.file, input.sdkKey, input.text) ?: return null
        return CredoResult(input, issues)
    }

    override fun apply(file: PsiFile, result: CredoResult, holder: AnnotationHolder) {
        if (!CredoSettings.getInstance(file.project).state.enabled || file.text != result.input.text) return
        for (issue in result.issues) {
            if (!CredoDiagnostics.matchesFile(result.input.mixRoot, result.input.file, issue.filename)) continue
            val (start, end) = CredoRanges.offsets(result.input.text, issue.line, issue.column, issue.columnEnd)
                ?: continue
            holder.newAnnotation(HighlightSeverity.WARNING, CredoDiagnostics.message(issue))
                .range(TextRange(start, end)).create()
        }
    }
}

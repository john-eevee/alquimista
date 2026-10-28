package dev.alquimista.expert.testing

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Paths

data class ExUnitStacktraceLocation(val path: String, val line: Int, val start: Int, val end: Int)

object ExUnitStacktrace {
    private val location = Regex("^\\s*(.+?\\.exs?):(\\d+)(?::\\d+)?(?::|\\s|$)")
    fun parse(line: String): ExUnitStacktraceLocation? {
        val match = location.find(line) ?: return null
        val number = match.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
        return ExUnitStacktraceLocation(match.groupValues[1], number, match.groups[1]!!.range.first, match.groups[2]!!.range.last + 1)
    }
}

class ExUnitStacktraceFilter(private val project: Project) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val location = ExUnitStacktrace.parse(line) ?: return null
        val root = project.basePath ?: return null
        val path = runCatching { Paths.get(root).resolve(location.path).normalize().toString() }.getOrNull() ?: return null
        val file = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        val offset = entireLength - line.length
        return Filter.Result(offset + location.start, offset + location.end, OpenFileHyperlinkInfo(project, file, location.line - 1))
    }
}

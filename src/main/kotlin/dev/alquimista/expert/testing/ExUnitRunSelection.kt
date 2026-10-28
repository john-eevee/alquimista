package dev.alquimista.expert.testing

import org.jdom.Element

data class ExUnitRunSelection(val path: String?, val line: Int?, val module: String?) {
    fun write(element: Element) {
        if (path != null) element.setAttribute("testPath", path) else element.removeAttribute("testPath")
        if (line != null) element.setAttribute("testLine", line.toString()) else element.removeAttribute("testLine")
        if (module != null) element.setAttribute("testModule", module) else element.removeAttribute("testModule")
    }

    companion object {
        fun forTarget(path: String, target: ExUnitTarget): ExUnitRunSelection =
            if (target.kind == ExUnitTargetKind.MODULE) ExUnitRunSelection(path, null, target.displayName)
            else ExUnitRunSelection(path, target.line, null)

        fun read(element: Element) = ExUnitRunSelection(
            element.getAttributeValue("testPath")?.takeIf(String::isNotBlank),
            element.getAttributeValue("testLine")?.toIntOrNull(),
            element.getAttributeValue("testModule")?.takeIf(String::isNotBlank),
        )
    }
}

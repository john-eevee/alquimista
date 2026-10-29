package ex.alquimista.expert.grammar.psi

import com.intellij.psi.tree.IElementType
import ex.alquimista.expert.grammar.ExLanguage

class ExTokenType(debugName: String) : IElementType(debugName, ExLanguage) {
    override fun toString(): String = "ExTokenType.${super.toString()}"
}

package dev.yaee.expert.grammar.psi

import com.intellij.psi.tree.IElementType
import dev.yaee.expert.grammar.ExLanguage

class ExTokenType(debugName: String) : IElementType(debugName, ExLanguage) {
    override fun toString(): String = "ExTokenType.${super.toString()}"
}

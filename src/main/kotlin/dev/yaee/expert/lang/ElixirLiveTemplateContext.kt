package dev.yaee.expert.lang

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

class ElixirLiveTemplateContext : TemplateContextType("Elixir") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean =
        templateActionContext.file.language == ElixirLanguage.INSTANCE
}

package dev.alquimista.expert.lang

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class ElixirLanguage private constructor() : Language("Elixir") {
    companion object {
        @JvmField
        val INSTANCE = ElixirLanguage()
    }
}

class ElixirFileType private constructor() : LanguageFileType(ElixirLanguage.INSTANCE) {
    override fun getName(): String = "Elixir"
    override fun getDescription(): String = "Elixir source file"
    override fun getDefaultExtension(): String = "ex"
    override fun getIcon(): Icon = ElixirIcons.FILE

    companion object {
        @JvmField
        val INSTANCE = ElixirFileType()
    }
}

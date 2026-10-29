package dev.alquimista.expert.lang

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class ElixirColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "Elixir"
    override fun getIcon(): Icon = ElixirIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = ElixirSyntaxHighlighter()
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = emptyMap()
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor("Keyword and macro", ElixirColors.KEYWORD),
        AttributesDescriptor("Module", ElixirColors.MODULE),
        AttributesDescriptor("Module attribute", ElixirColors.MODULE_ATTRIBUTE),
        AttributesDescriptor("Function declaration", ElixirColors.FUNCTION_DECLARATION),
        AttributesDescriptor("Function call", ElixirColors.FUNCTION_CALL),
        AttributesDescriptor("Comment", ElixirColors.COMMENT),
        AttributesDescriptor("String", ElixirColors.STRING),
        AttributesDescriptor("Number", ElixirColors.NUMBER),
        AttributesDescriptor("Atom and keyword key", ElixirColors.ATOM),
        AttributesDescriptor("Operator", ElixirColors.OPERATOR),
        AttributesDescriptor("Parentheses", ElixirColors.PARENTHESES),
        AttributesDescriptor("Brackets", ElixirColors.BRACKETS),
        AttributesDescriptor("Braces", ElixirColors.BRACES),
        AttributesDescriptor("Comma", ElixirColors.COMMA),
        AttributesDescriptor("Dot", ElixirColors.DOT),
        AttributesDescriptor("Semicolon", ElixirColors.SEMICOLON),
        AttributesDescriptor("Valid escape", ElixirColors.VALID_ESCAPE),
        AttributesDescriptor("Invalid escape", ElixirColors.INVALID_ESCAPE),
        AttributesDescriptor("Bad character", ElixirColors.BAD_CHARACTER),
    )

    override fun getDemoText(): String = """# A small Elixir example
defmodule Example.Greeter do
  def greet(name), do: format(name)

  defp format(name) do
    IO.puts("Hello, #{name}!")
    {:ok, [1, 2]}
  end
end
"""
}

class HeexColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "HEEx"
    override fun getIcon(): Icon = ElixirIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = HeexSyntaxHighlighter()
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = emptyMap()
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor("Tag punctuation", HeexColors.TAG_DELIMITER),
        AttributesDescriptor("Tag name", HeexColors.TAG_NAME),
        AttributesDescriptor("Attribute", HeexColors.ATTRIBUTE),
        AttributesDescriptor("String", HeexColors.STRING),
        AttributesDescriptor("Comment", HeexColors.COMMENT),
        AttributesDescriptor("Entity", HeexColors.ENTITY),
        AttributesDescriptor("Expression delimiter", HeexColors.EXPRESSION_DELIMITER),
        AttributesDescriptor("Elixir keyword and macro", HeexColors.KEYWORD),
        AttributesDescriptor("Elixir module", HeexColors.MODULE),
        AttributesDescriptor("Elixir module attribute and assign", HeexColors.MODULE_ATTRIBUTE),
        AttributesDescriptor("Elixir function declaration", HeexColors.FUNCTION_DECLARATION),
        AttributesDescriptor("Elixir function call", HeexColors.FUNCTION_CALL),
        AttributesDescriptor("Elixir number", HeexColors.NUMBER),
        AttributesDescriptor("Elixir atom and keyword key", HeexColors.ATOM),
        AttributesDescriptor("Elixir operator", HeexColors.OPERATOR),
        AttributesDescriptor("Elixir parentheses", HeexColors.PARENTHESES),
        AttributesDescriptor("Elixir brackets", HeexColors.BRACKETS),
        AttributesDescriptor("Elixir braces", HeexColors.BRACES),
        AttributesDescriptor("Elixir comma", HeexColors.COMMA),
        AttributesDescriptor("Elixir dot", HeexColors.DOT),
        AttributesDescriptor("Elixir semicolon", HeexColors.SEMICOLON),
        AttributesDescriptor("Elixir valid escape", HeexColors.VALID_ESCAPE),
        AttributesDescriptor("Elixir invalid escape", HeexColors.INVALID_ESCAPE),
    )

    override fun getDemoText(): String = """<.card :if={@visible} class="greeting">
  Hello, {@name}!
  <%= Enum.map(@items, fn item -> item end) %>
</.card>"""
}

class EexColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "EEx"
    override fun getIcon(): Icon = ElixirIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = EexSyntaxHighlighter()
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = emptyMap()
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor("Expression delimiter (Elixir colors use the Elixir page)", EexColors.DELIMITER),
    )

    override fun getDemoText(): String = """Hello, <%= Enum.join(@names, ", ") %>!
<%!-- A template comment --%>"""
}

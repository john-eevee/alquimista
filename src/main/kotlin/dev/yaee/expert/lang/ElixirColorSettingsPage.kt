package dev.yaee.expert.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
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

/** HEEx currently uses TextMate's shared theme keys, which also affect other TextMate languages. */
class HeexColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "HEEx (shared TextMate colors)"
    override fun getIcon(): Icon = ElixirIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = PlainSyntaxHighlighter()
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor("Tag punctuation", Defaults.MARKUP_TAG),
        AttributesDescriptor("Attribute", Defaults.MARKUP_ATTRIBUTE),
        AttributesDescriptor("Keyword", Defaults.KEYWORD),
        AttributesDescriptor("String", Defaults.STRING),
        AttributesDescriptor("Number", Defaults.NUMBER),
        AttributesDescriptor("Comment", Defaults.LINE_COMMENT),
        AttributesDescriptor("Function", Defaults.FUNCTION_CALL),
        AttributesDescriptor("Tag name and module", Defaults.CLASS_NAME),
        AttributesDescriptor("Embedded code", Defaults.TEMPLATE_LANGUAGE_COLOR),
    )
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = mapOf(
        "tag" to Defaults.MARKUP_TAG,
        "tagName" to Defaults.CLASS_NAME,
        "attribute" to Defaults.MARKUP_ATTRIBUTE,
        "string" to Defaults.STRING,
        "keyword" to Defaults.KEYWORD,
        "function" to Defaults.FUNCTION_CALL,
        "embedded" to Defaults.TEMPLATE_LANGUAGE_COLOR,
    )
    override fun getDemoText(): String = """<tag><</tag><tagName>div</tagName> <attribute>class</attribute>=<string>"greeting"</string><tag>></tag>
  <embedded><%= </embedded><keyword>if</keyword> @name <keyword>do</keyword><embedded> %></embedded>
    <embedded><%= </embedded><function>greet</function>(@name)<embedded> %></embedded>
  <embedded><% </embedded><keyword>end</keyword><embedded> %></embedded>
<tag></</tag><tagName>div</tagName><tag>></tag>"""
}

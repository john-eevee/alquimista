package dev.yaee.expert.lang

import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import dev.murek.elixirij.lang.*

class ElixirCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "#"
    override fun getBlockCommentPrefix(): String? = null
    override fun getBlockCommentSuffix(): String? = null
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}

class ElixirBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = arrayOf(
        BracePair(EX_LPAREN, EX_RPAREN, false),
        BracePair(EX_LBRACKET, EX_RBRACKET, false),
        BracePair(EX_LBRACE, EX_RBRACE, false),
        BracePair(EX_PERCENT_LBRACE, EX_RBRACE, false),
        BracePair(EX_LT_LT, EX_GT_GT, false),
        BracePair(EX_INTERPOLATION_START, EX_INTERPOLATION_END, false),
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true
    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}

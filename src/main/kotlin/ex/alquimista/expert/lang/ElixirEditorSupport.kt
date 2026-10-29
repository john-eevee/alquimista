package ex.alquimista.expert.lang

import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import ex.alquimista.expert.grammar.*

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

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean =
        contextType == null || contextType in setOf(
            TokenType.WHITE_SPACE, EX_EOL, EX_COMMENT, EX_COMMA, EX_SEMICOLON,
            EX_RPAREN, EX_RBRACKET, EX_RBRACE, EX_GT_GT, EX_INTERPOLATION_END
        )

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}

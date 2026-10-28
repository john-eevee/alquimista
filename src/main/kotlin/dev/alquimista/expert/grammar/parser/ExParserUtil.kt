package dev.alquimista.expert.grammar.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.tree.IElementType
import dev.alquimista.expert.grammar.EX_ATOM
import dev.alquimista.expert.grammar.EX_ATOM_QUOTED
import dev.alquimista.expert.grammar.EX_CHAR
import dev.alquimista.expert.grammar.EX_CHARLIST
import dev.alquimista.expert.grammar.EX_CHARLIST_BEGIN
import dev.alquimista.expert.grammar.EX_CHARLIST_HEREDOC
import dev.alquimista.expert.grammar.EX_COLON
import dev.alquimista.expert.grammar.EX_COMMA
import dev.alquimista.expert.grammar.EX_COMMENT
import dev.alquimista.expert.grammar.EX_EOL
import dev.alquimista.expert.grammar.EX_FALSE
import dev.alquimista.expert.grammar.EX_FAT_ARROW
import dev.alquimista.expert.grammar.EX_FLOAT
import dev.alquimista.expert.grammar.EX_GT_GT
import dev.alquimista.expert.grammar.EX_HEREDOC
import dev.alquimista.expert.grammar.EX_INTEGER
import dev.alquimista.expert.grammar.EX_KW_IDENTIFIER
import dev.alquimista.expert.grammar.EX_LBRACE
import dev.alquimista.expert.grammar.EX_LBRACKET
import dev.alquimista.expert.grammar.EX_LPAREN
import dev.alquimista.expert.grammar.EX_LT_LT
import dev.alquimista.expert.grammar.EX_PERCENT_LBRACE
import dev.alquimista.expert.grammar.EX_PIPE
import dev.alquimista.expert.grammar.EX_PERCENT
import dev.alquimista.expert.grammar.EX_RBRACE
import dev.alquimista.expert.grammar.EX_RBRACKET
import dev.alquimista.expert.grammar.EX_RPAREN
import dev.alquimista.expert.grammar.EX_SIGIL
import dev.alquimista.expert.grammar.EX_STRING
import dev.alquimista.expert.grammar.EX_STRING_BEGIN
import dev.alquimista.expert.grammar.EX_TRUE
import dev.alquimista.expert.grammar.EX_NIL

/**
 * Parser helpers invoked from the generated grammar to keep lookaheads and fast paths centralized.
 */
object ExParserUtil {
    /**
     * Look ahead in a map body to decide whether we're starting a map update (`%{base | ...}`).
     *
     * Returns true if a top-level `|` is seen before any top-level `=>`, `,`, or `}`.
     */
    @JvmStatic
    fun mapUpdateStart(builder: PsiBuilder, level: Int): Boolean {
        if (!GeneratedParserUtilBase.recursion_guard_(builder, level, "mapUpdateStart")) return false
        val marker = builder.mark()
        var depth = 0

        while (true) {
            val type: IElementType = builder.tokenType ?: break

            if (depth == 0) {
                when (type) {
                    EX_KW_IDENTIFIER,
                    EX_COLON,
                    EX_FAT_ARROW,
                    EX_COMMA,
                    EX_RBRACE -> {
                        marker.rollbackTo()
                        return false
                    }

                    EX_PIPE -> {
                        marker.rollbackTo()
                        return true
                    }
                }
            }

            when (type) {
                EX_LPAREN,
                EX_LBRACKET,
                EX_LBRACE,
                EX_PERCENT_LBRACE,
                EX_LT_LT -> depth++

                EX_RPAREN,
                EX_RBRACKET,
                EX_RBRACE,
                EX_GT_GT -> if (depth > 0) depth--
            }

            builder.advanceLexer()
        }

        marker.rollbackTo()
        return false
    }

    /**
     * Look ahead in a map body to decide whether we're starting an assoc pair list (`key => value`).
     *
     * Returns true if a top-level `=>` is seen before any top-level `|`, `,`, or `}`.
     */
    @JvmStatic
    fun mapAssocPairStart(builder: PsiBuilder, level: Int): Boolean {
        if (!GeneratedParserUtilBase.recursion_guard_(builder, level, "mapAssocPairStart")) return false
        return scanForTopLevel(builder, EX_FAT_ARROW, EX_PIPE)
    }

    /**
     * Parse "container expressions" with a fast path for literal-like constructs.
     *
     * If the fast parse succeeds, we ensure the next token is a container boundary (`,`, `}`, `)`,
     * `]`, `=>`, or EOF). Otherwise, we roll back and parse via the full `expression`.
     */
    @JvmStatic
    fun containerExprFast(builder: PsiBuilder, level: Int): Boolean {
        if (!GeneratedParserUtilBase.recursion_guard_(builder, level, "containerExprFast")) return false
        val marker = builder.mark()
        val tokenType = builder.tokenType
        val fastResult = when (tokenType) {
            EX_LBRACKET -> ExParser.list(builder, level + 1)
            EX_LBRACE -> ExParser.tuple(builder, level + 1)
            EX_PERCENT_LBRACE -> ExParser.map(builder, level + 1)
            EX_PERCENT -> ExParser.struct(builder, level + 1)
            EX_LT_LT -> ExParser.bitstring(builder, level + 1)
            EX_SIGIL -> ExParser.sigil(builder, level + 1)
            EX_HEREDOC -> ExParser.heredoc(builder, level + 1)
            EX_CHARLIST_HEREDOC -> ExParser.charlistHeredoc(builder, level + 1)
            EX_LPAREN -> ExParser.parenExpr(builder, level + 1)
            EX_ATOM, EX_ATOM_QUOTED, EX_COLON -> ExParser.atom(builder, level + 1)
            EX_INTEGER,
            EX_FLOAT,
            EX_CHAR,
            EX_STRING,
            EX_STRING_BEGIN,
            EX_CHARLIST,
            EX_CHARLIST_BEGIN,
            EX_TRUE,
            EX_FALSE,
            EX_NIL -> ExParser.literal(builder, level + 1)

            else -> false
        }

        if (fastResult && isContainerBoundary(builder)) {
            marker.drop()
            return true
        }

        marker.rollbackTo()
        return ExParser.expression(builder, level + 1)
    }


    /**
     * Scan tokens at top level, returning true if [target] is seen before [earlyStop].
     *
     * Stops on comma or closing brace when at depth 0. Rolls back to the starting position.
     */
    private fun scanForTopLevel(
        builder: PsiBuilder,
        target: IElementType,
        earlyStop: IElementType
    ): Boolean {
        val marker = builder.mark()
        var depth = 0

        while (true) {
            val type: IElementType = builder.tokenType ?: break

            if (depth == 0) {
                if (type == earlyStop) {
                    marker.rollbackTo()
                    return false
                }
                if (type == target) {
                    marker.rollbackTo()
                    return true
                }
                if (type == EX_COMMA || type == EX_RBRACE) {
                    marker.rollbackTo()
                    return false
                }
            }

            when (type) {
                EX_LPAREN,
                EX_LBRACKET,
                EX_LBRACE,
                EX_PERCENT_LBRACE,
                EX_LT_LT -> depth++

                EX_RPAREN,
                EX_RBRACKET,
                EX_RBRACE,
                EX_GT_GT -> if (depth > 0) depth--
            }

            builder.advanceLexer()
        }

        marker.rollbackTo()
        return false
    }

    /**
     * Check whether the next non-whitespace/comment token is a container boundary.
     *
     * The check is performed on a rollback marker so it doesn't consume input.
     */
    private fun isContainerBoundary(builder: PsiBuilder): Boolean {
        val marker = builder.mark()
        while (true) {
            val tokenType = builder.tokenType ?: break
            if (tokenType != EX_EOL && tokenType != EX_COMMENT) break
            builder.advanceLexer()
        }
        val tokenType = builder.tokenType
        val result = tokenType == null ||
            tokenType == EX_COMMA ||
            tokenType == EX_RBRACE ||
            tokenType == EX_RPAREN ||
            tokenType == EX_RBRACKET ||
            tokenType == EX_FAT_ARROW
        marker.rollbackTo()
        return result
    }
}

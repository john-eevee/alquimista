package dev.alquimista.expert.lang

import com.intellij.lang.Language

class HeexLanguage private constructor() : Language("HEEx") {
    companion object {
        @JvmField
        val INSTANCE = HeexLanguage()
    }
}

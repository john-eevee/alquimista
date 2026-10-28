import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.intellij.platform.grammarkit")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2026.2.3")
        grammarKit()
        jflex()
        bundledPlugin("org.jetbrains.plugins.textmate")
        bundledPlugin("intellij.testRunner.plugin")
        testFramework(TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here, for example:
        // bundledPlugin("com.intellij.java")
    }
}

val generatedGrammarRoot = layout.buildDirectory.dir("generated/grammarkit")
sourceSets.main { java.srcDir(generatedGrammarRoot) }

tasks {
    generateParser {
        sourceFile.set(file("src/main/grammars/Elixir.bnf"))
        targetRootOutputDir.set(generatedGrammarRoot)
        pathToParser.set("/dev/alquimista/expert/grammar/parser/ExParser.java")
        pathToPsiRoot.set("/dev/alquimista/expert/grammar/psi")
    }
    generateLexer {
        sourceFile.set(file("src/main/grammars/Elixir.flex"))
        targetRootOutputDir.set(generatedGrammarRoot)
        pathToClass.set("/dev/alquimista/expert/grammar/lexer/_ExLexer.java")
        dependsOn(generateParser)
    }
    compileKotlin { dependsOn(generateLexer) }
}

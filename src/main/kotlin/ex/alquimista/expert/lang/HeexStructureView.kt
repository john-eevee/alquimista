package ex.alquimista.expert.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.ide.structureView.*
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

class HeexElement(node: ASTNode) : ASTWrapperPsiElement(node)

class HeexStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder =
        object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                StructureViewModelBase(
                    psiFile,
                    editor,
                    HeexStructureElement(psiFile)
                ).withSuitableClasses(HeexElement::class.java)
        }
}

class HeexStructureElement(private val element: PsiElement) : StructureViewTreeElement {
    override fun getValue(): Any = element
    override fun getChildren(): Array<StructureViewTreeElement> = element.children
        .filterIsInstance<HeexElement>()
        .map(::HeexStructureElement).toTypedArray()

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = if (element is PsiFile) element.name else {
            val name = element.node.findChildByType(HEEX_TAG_NAME)?.text.orEmpty()
            "<$name>"
        }

        override fun getLocationString(): String? = null
        override fun getIcon(unused: Boolean): Icon? = null
    }

    override fun navigate(requestFocus: Boolean) {
        val file = element.containingFile?.virtualFile ?: return
        OpenFileDescriptor(element.project, file, element.textOffset).navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = element.isValid && element.containingFile?.virtualFile != null
    override fun canNavigateToSource(): Boolean = canNavigate()
}

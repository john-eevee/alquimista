package dev.yaee.expert.credo

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "YaeeCredoSettings", storages = [Storage("yaee-credo.xml")])
class CredoSettings : PersistentStateComponent<CredoSettings.State> {
    data class State(var enabled: Boolean = false)

    private var value = State()
    override fun getState(): State = value
    override fun loadState(state: State) { value = state }

    companion object {
        fun getInstance(project: Project): CredoSettings = project.getService(CredoSettings::class.java)
    }
}

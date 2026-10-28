package dev.yaee.expert.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "YaeeExpertSettings", storages = [Storage("yaee-expert.xml")])
class ExpertSettings : PersistentStateComponent<ExpertSettings.State> {
    data class State(
        var expertPath: String = "",
        var minimumWorkspaceSymbolQueryLength: Int = DEFAULT_MINIMUM_WORKSPACE_SYMBOL_QUERY_LENGTH,
    )

    private var persistedState = State()

    var expertPath: String
        get() = persistedState.expertPath
        set(value) {
            persistedState.expertPath = ExpertPathChange.normalize(value)
        }

    fun updateExpertPath(value: String): Boolean {
        if (!ExpertPathChange.hasChanged(expertPath, value)) return false
        expertPath = value
        return true
    }

    var minimumWorkspaceSymbolQueryLength: Int
        get() = persistedState.minimumWorkspaceSymbolQueryLength
        set(value) {
            persistedState.minimumWorkspaceSymbolQueryLength = value.coerceAtLeast(1)
        }

    override fun getState(): State = persistedState

    override fun loadState(state: State) {
        persistedState = state
    }

    companion object {
        const val DEFAULT_MINIMUM_WORKSPACE_SYMBOL_QUERY_LENGTH = 2

        fun getInstance(): ExpertSettings =
            ApplicationManager.getApplication().getService(ExpertSettings::class.java)
    }
}

object ExpertPathChange {
    fun normalize(path: String): String = path.trim()

    fun hasChanged(current: String, proposed: String): Boolean = normalize(current) != normalize(proposed)
}

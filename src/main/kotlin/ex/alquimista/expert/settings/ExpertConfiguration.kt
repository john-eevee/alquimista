package ex.alquimista.expert.settings

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

internal object ExpertConfiguration {
    fun toJson(state: ExpertSettings.State): JsonObject = JsonObject().apply {
        add("workspaceSymbols", JsonObject().apply {
            addProperty("minQueryLength", state.minimumWorkspaceSymbolQueryLength)
        })
        addProperty("logLevel", state.logLevel)
        addProperty("fileLogLevel", state.fileLogLevel)
        addProperty("elixirSourcePath", state.elixirSourcePath.trim().ifEmpty { null })
        addProperty("elixirExecutablePath", state.elixirExecutablePath.trim().ifEmpty { null })
        addProperty("erlangExecutablePath", state.erlangExecutablePath.trim().ifEmpty { null })
        addProperty("autoFetchDependencies", state.autoFetchDependencies)
        addProperty("compileOnType", state.compileOnType)
        add("additionalWatchedExtensions", JsonArray().apply {
            add(JsonPrimitive(".heex"))
            add(JsonPrimitive(".eex"))
        })
    }
}

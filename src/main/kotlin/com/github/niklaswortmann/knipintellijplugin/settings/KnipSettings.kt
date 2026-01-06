package com.github.niklaswortmann.knipintellijplugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistent settings for the Knip plugin.
 * Settings are stored per-project.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "KnipSettings",
    storages = [Storage("knip.xml")]
)
class KnipSettings : PersistentStateComponent<KnipSettings.State> {

    data class State(
        var enabled: Boolean = true,
        var nodePath: String = "",
        var npxPath: String = "",
        var serverArguments: String = "--stdio"
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    var enabled: Boolean
        get() = myState.enabled
        set(value) { myState.enabled = value }

    var nodePath: String
        get() = myState.nodePath
        set(value) { myState.nodePath = value }

    var npxPath: String
        get() = myState.npxPath
        set(value) { myState.npxPath = value }

    var serverArguments: String
        get() = myState.serverArguments
        set(value) { myState.serverArguments = value }

    companion object {
        fun getInstance(project: Project): KnipSettings = project.service()
    }
}

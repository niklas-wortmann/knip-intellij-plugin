package com.github.niklaswortmann.knipintellijplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level settings for the Knip plugin.
 * These settings persist across all projects.
 */
@Service(Service.Level.APP)
@State(
    name = "KnipApplicationSettings",
    storages = [Storage("knip-application.xml")]
)
class KnipApplicationSettings : PersistentStateComponent<KnipApplicationSettings.State> {

    data class State(
        var dismissedPluginSuggestion: Boolean = false
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    var dismissedPluginSuggestion: Boolean
        get() = myState.dismissedPluginSuggestion
        set(value) { myState.dismissedPluginSuggestion = value }

    companion object {
        fun getInstance(): KnipApplicationSettings = 
            ApplicationManager.getApplication().getService(KnipApplicationSettings::class.java)
    }
}

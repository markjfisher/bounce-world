package bw

import bw.Model.connectToServer
import io.kvision.Application
import io.kvision.BootstrapModule
import io.kvision.CoreModule
import io.kvision.module
import io.kvision.panel.root
import io.kvision.startApplication
import io.kvision.theme.Theme
import io.kvision.theme.ThemeManager
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

val AppScope = CoroutineScope(window.asCoroutineDispatcher())

class WebApp : Application() {
    init {
        ThemeManager.init(initialTheme = Theme.DARK, remember = true)
        io.kvision.require("./css/bw.css")
    }

    override fun start() {
        root("kvapp") {
            add(NavBar)
            add(MainContainer)
        }
        AppScope.launch {
            Model.getWorldData()
            connectToServer()
        }
    }
}

fun main() {
    startApplication(::WebApp, module.hot, BootstrapModule, CoreModule)
}
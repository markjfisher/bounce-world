package bw

import bw.Model.connectToServer
import io.kvision.Application
import io.kvision.BootstrapModule
import io.kvision.CoreModule
import io.kvision.panel.root
import io.kvision.remote.registerRemoteTypes
import io.kvision.startApplication
import io.kvision.theme.Theme
import io.kvision.theme.ThemeManager
import io.kvision.utils.useModule
import kotlinx.browser.window
import kotlin.js.JsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

val AppScope = CoroutineScope(window.asCoroutineDispatcher())

@JsModule("../../modules/css/bw.css")
external object bwCss

class WebApp : Application() {
    init {
        ThemeManager.init(initialTheme = Theme.DARK, remember = true)
        useModule(bwCss)
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
    registerRemoteTypes()
    startApplication(
        ::WebApp,
        js("import.meta.webpackHot").unsafeCast<dynamic>(),
        BootstrapModule,
        CoreModule,
    )
}

package bw

import io.kvision.html.h1
import io.kvision.html.p
import io.kvision.panel.SimplePanel
import io.kvision.panel.simplePanel

object MainContainer: SimplePanel(className = "container") {
    init {
        simplePanel(className = "row") {
            add(ServerInfo)
        }
    }
}
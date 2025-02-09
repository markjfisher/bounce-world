package bw

import io.kvision.html.link
import io.kvision.panel.SimplePanel
import io.kvision.panel.simplePanel

object NavBar: SimplePanel(className = "navbar navbar-expand-lg sticky-top bg-primary") {
    init {
        setAttribute("data-bs-theme", "dark")
        simplePanel(className = "container") {
            link(className = "navbar-brand", label = "Bouncy World", url = "#")
        }

    }
}
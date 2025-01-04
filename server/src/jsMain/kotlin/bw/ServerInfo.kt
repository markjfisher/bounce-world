package bw

import io.kvision.html.h1
import io.kvision.html.li
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.html.textNode
import io.kvision.html.ul
import io.kvision.panel.SimplePanel
import io.kvision.panel.simplePanel
import io.kvision.state.bind

object ServerInfo: SimplePanel() {
    init {
        simplePanel(className = "page-header mt-4") {
            h1(content = "Server Stats")
            p(className = "lead", content = "What's bouncing in the server?")
        }
        simplePanel(className = "bs-component").bind(Model.worldData) { worldData ->
            ul(className = "list-group") {
                li(className = "list-group-item d-flex justify-content-between align-items-center") {
                    val myText = "Client Count"
                    textNode(content = myText) // can also use "+myText" as "+" is the textNode function shortcut via unaryPlus
                    span(className = "badge bg-primary rounded-pill") {
                        content = worldData.clients?.count()?.toString() ?: "0"
                    }
                }
                li(className = "list-group-item d-flex justify-content-between align-items-center") {
                    textNode(content = "Active Bodies")
                    span(className = "badge bg-primary rounded-pill") {
                        content = worldData.bodies?.count()?.toString() ?: "0"
                    }
                }
                li(className = "list-group-item d-flex justify-content-between align-items-center") {
                    val myText = "Frozen?"
                    textNode(content = myText) // can also use "+myText" as "+" is the textNode function shortcut via unaryPlus
                    span(className = "badge bg-primary rounded-pill") {
                        content = worldData.isFrozen?.toString() ?: "false"
                    }
                }
            }
        }

        simplePanel(className = "bs-component").bind(Model.worldData) { worldData ->
            ul(className = "list-group") {
                li(className = "list-group-item d-flex justify-content-between align-items-center") {
                    textNode(content = "Uptime")
                    span(className = "badge bg-primary") {
                        content = worldData.upTime ?: "N/A"
                    }
                }
            }
        }


//        simplePanel(className = "card text-white bg-primary mt-4 mb-3 border-secondary") {
//            simplePanel(className = "card-header") {
//                h4(className = "my-3 text-white", content = "card header not p")
//            }
//            simplePanel(className = "card-body bg-primary") {
//                h1(className = "card-title") {
//                    p(className = "text-muted", content = "card body/title")
//                }
//                ul(className = "list-unstyled mt-3 mb-4 text-light") {
//                    li(content = "item 1")
//                    li(content = "item 2")
//                    li(content = "item 3")
//                }
//            }
//        }
    }
}
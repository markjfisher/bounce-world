package routing

import bw.CommandProcessorAttributeKey
import bw.WorldAttributeKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val commandProcessor = attributes[CommandProcessorAttributeKey]
    val world = attributes[WorldAttributeKey]

    routing {
        get("/") {
            call.respondText("Bounce World 2")
        }

        get("/add/{size}") {
            val size = call.parameters["size"]?.toIntOrNull()
            if (size != null) {
                val response = commandProcessor.process("add/$size")
                call.respondBytes(response)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid size parameter")
            }
        }

        get("/reset") {
            val response = commandProcessor.process("reset")
            call.respondBytes(response)
        }

        get("/inc") {
            val response = commandProcessor.process("inc")
            call.respondBytes(response)
        }

        get("/dec") {
            val response = commandProcessor.process("dec")
            call.respondBytes(response)
        }

        get("/cmd/put/{clientId}/{cmd}") {
            val clientId = call.parameters["clientId"]
            val cmd = call.parameters["cmd"]
            if (clientId != null && cmd != null) {
                val response = commandProcessor.process("cmd/put/$clientId/$cmd")
                call.respondBytes(response)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
            }
        }

        get("/cmd/broadcast/{clientId}/{time}/{message}") {
            val clientId = call.parameters["clientId"]
            val time = call.parameters["time"]
            val message = call.parameters["message"]
            if (clientId != null && time != null && message != null) {
                val response = commandProcessor.process("cmd/broadcast/$clientId/$time/$message")
                call.respondBytes(response)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
            }
        }

        get("/freeze") {
            val response = commandProcessor.process("freeze")
            call.respondBytes(response)
        }

        get("/status") {
            val response = commandProcessor.process("status")
            call.respondBytes(response)
        }

        get("/w/{clientId}") {
            val clientId = call.parameters["clientId"]?.toIntOrNull()
            if (clientId != null) {
                val response = commandProcessor.process("w/$clientId")
                call.respondBytes(response)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid clientId parameter")
            }
        }

        get("/ws") {
            val response = commandProcessor.process("ws")
            call.respondBytes(response)
        }
    }}

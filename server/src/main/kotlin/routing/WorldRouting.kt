package routing

import bw.WorldCommandProcessorAttributeKey
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.worldRouting() {
    // everything is proxied to the CommandProcessor. Will have to decide if we call it WorldCommandProcessor, so we can have similar for Shapes and Clients
    val commandProcessor = attributes[WorldCommandProcessorAttributeKey]

    routing {
        get("/") {
            call.respondText("Bounce World 2")
        }

        get("/w/{clientId}") {
            val clientId = call.parameters["clientId"]?.toIntOrNull()
            if (clientId != null) {
                val response  = commandProcessor.getWorldData(clientId)
                call.respondBytes(response, contentType = ContentType.Application.OctetStream)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid clientId parameter")
            }
        }

        get("/ws") {
            val response = commandProcessor.getWorldState()
            call.respondBytes(response, contentType = ContentType.Application.OctetStream)
        }

        get("/status") {
            val status = commandProcessor.getStatus()
            call.respond(HttpStatusCode.OK, status)
        }

        get("/add/{size}") {
            val size = call.parameters["size"]?.toIntOrNull()
            if (size != null) {
                val response = commandProcessor.addBody(size)
                call.respondBytes(response, contentType = ContentType.Application.OctetStream)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid size parameter")
            }
        }

        get("/reset") {
            val response = commandProcessor.resetWorld()
            call.respondBytes(response, contentType = ContentType.Application.OctetStream)
        }

        get("/inc") {
            val response = commandProcessor.increaseSpeed()
            call.respondBytes(response, contentType = ContentType.Application.OctetStream)
        }

        get("/dec") {
            val response = commandProcessor.decreaseSpeed()
            call.respondBytes(response, contentType = ContentType.Application.OctetStream)
        }

        get("/freeze") {
            val response = commandProcessor.toggleFreeze()
            call.respondBytes(response, contentType = ContentType.Application.OctetStream)
        }

        get("/cmd/put/{clientId}/{cmd}") {
            val clientId = call.parameters["clientId"]
            val cmd = call.parameters["cmd"]
            if (clientId != null && cmd != null) {
                val response = commandProcessor.clientCommand(clientId, cmd)
                call.respondBytes(response, contentType = ContentType.Application.OctetStream)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
            }
        }

        get("/cmd/get/{clientId}") {
            val clientId = call.parameters["clientId"]
            if (clientId != null) {
                val response = commandProcessor.fetchCommands(clientId)
                call.respondBytes(response, contentType = ContentType.Application.OctetStream)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
            }
        }

        get("/cmd/broadcast/{clientId}/{time}/{message}") {
            val clientId = call.parameters["clientId"]
            val time = call.parameters["time"]
            val message = call.parameters["message"]
            if (clientId != null && time != null && message != null) {
                val response = commandProcessor.broadcastCommand(clientId, time, message)
                call.respondBytes(response, contentType = ContentType.Application.OctetStream)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
            }
        }

        get("/msg") {
            val latestMessage = commandProcessor.getLatestMessage()
            call.respondText(latestMessage, contentType = ContentType.Text.Plain)
        }

        get("/get-clients") {
            val clients = commandProcessor.getClients()
            call.respond(HttpStatusCode.OK, clients)
        }

        get("/who") {
            val fixedWhoString = commandProcessor.who()
            call.respondText(fixedWhoString, contentType = ContentType.Text.Plain)
        }
    }
}

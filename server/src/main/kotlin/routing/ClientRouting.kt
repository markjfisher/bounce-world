package routing

import bw.ClientCommandProcessorAttributeKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import logger

fun Application.clientRouting() {
    logger.info("Creating routing for clients")
    val commandProcessor = attributes[ClientCommandProcessorAttributeKey]

    routing {
        route("/client") {
            post {
                val gameClientInfoString = call.receiveText()
                val parts = gameClientInfoString.split(",")
                // Allow simple comma separated fields so the client doesn't have to write JSON for the sake of it
                if (parts.size != 4) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Incorrect data format, should have: 'name,version,width,height'"
                    )
                    return@post
                }
                val name = parts[0]
                val version = parts[1].toIntOrNull()
                val screenWidth = parts[2].toIntOrNull()
                val screenHeight = parts[3].toIntOrNull()

                if (version == null || screenWidth == null || screenHeight == null || name.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid Parameters")
                    return@post
                }

                val gameClient = commandProcessor.addClient(parts[0], version, screenWidth, screenHeight)
                call.respondBytes(byteArrayOf(gameClient.id.toByte()), status = HttpStatusCode.Created)
            }
        }
    }
}
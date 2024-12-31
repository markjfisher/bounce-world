package routing

import bw.ShapesCommandProcessorAttributeKey
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.shapesRouting() {
    val commandProcessor = attributes[ShapesCommandProcessorAttributeKey]

    routing {
        route("/shapes") {
            get("data") {
                val response = commandProcessor.getShapesData()
                call.respondBytes(response, contentType = ContentType.Application.OctetStream)
            }

            get("count") {
                // allow the client to pre-calculate things by knowing number of shapes ahead of reading data.
                // return as a byte so the client doesn't need to convert ascii number to an int
                val count = commandProcessor.getShapesCount().toByte()
                // println("returning count as $count")
                call.respondBytes(byteArrayOf(count), contentType = ContentType.Application.OctetStream)
            }
        }
    }
}
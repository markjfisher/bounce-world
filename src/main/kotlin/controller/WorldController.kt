package controller

import config.WorldConfiguration
import domain.DelayInfo
import domain.World
import geometry.Point
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.MediaType.TEXT_PLAIN
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import jakarta.validation.Valid
import kotlin.math.roundToInt

@Controller("/")
open class WorldController(
    private val world: World,
    private val config: WorldConfiguration
) {
//    private val channelsContext = CoroutineScope(Dispatchers.Default).coroutineContext

    @Get("w/{clientId}", produces = [MediaType.APPLICATION_OCTET_STREAM])
    fun getWorldData(clientId: Int): HttpResponse<ByteArray> {
        // val client = world.getClient(clientId) ?: return HttpResponse.notFound()
        world.clientHeartbeats[clientId] = System.currentTimeMillis()
        val data = try {
            val csv = asCSV(clientId)
            // println("sending ${client.name}: $csv")
            // if there are no bodies in the view, we will return a value of 0 (as byte)
            if (csv.isNotEmpty()) csv.split(",").map { it.toInt().toByte() }.toByteArray() else byteArrayOf(0)
        } catch (e: Exception) {
            println("ERROR processing client ${clientId}: ${e.message}, sending 0")
            byteArrayOf(0)
        }
        val stepNumber = world.simulator.currentStep.toByte()
        val appStatus = world.calculateStatus(clientId)
        return HttpResponse.ok(byteArrayOf(stepNumber, appStatus) + data)
    }

    @Post("config/delay", produces = [TEXT_PLAIN], consumes = [APPLICATION_JSON])
    open fun setConfigDelay(@Valid @Body delayInfo: DelayInfo): HttpResponse<String> {
        world.setDelay(delayInfo.delay)
        return HttpResponse.ok("configured delay to ${delayInfo.delay} milliseconds")
    }

    // keep it as generating a string so we can print it if needed
    private fun asCSV(clientId: Int): String {
        val visibleShapes = world.currentClientVisibleShapes[clientId]
        if (!visibleShapes.isNullOrEmpty()) {
//            println("client $clientId visible shapes ---------------")
            val gameClient = world.getClient(clientId)!!
            val clientData = visibleShapes.joinToString(",") { vs ->
                // vs is in world coordinates, remove the client's top left corner position to get it relative to the client's real dimensions
                val adjustedToClientViewPosition = vs.position - gameClient.worldBounds.first

                // now scale down to the client's screen size
                val scaling = 1f * gameClient.screenSize.width / config.width
                val scaledToClientViewPosition = Point(
                    (adjustedToClientViewPosition.x * scaling).roundToInt(),
                    (adjustedToClientViewPosition.y * scaling).roundToInt()
                )
//                println("scaled from $vs to $scaledToClientViewPosition")

                // now convert to a comma delimited string
                "${vs.shapeId},${scaledToClientViewPosition.x},${scaledToClientViewPosition.y}"
            }
            // prepend with the count of shapes we need to read from the string
            return "${visibleShapes.size},$clientData"
        } else {
            return ""
        }
    }

}
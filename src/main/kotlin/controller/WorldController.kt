package controller

import domain.World
import geometry.Point
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import kotlin.math.roundToInt

@Controller("/world")
open class WorldController(private val world: World) {
    @Get("{clientId}", produces = [MediaType.TEXT_PLAIN])
    fun data(clientId: String): String {
        val visibleShapes = world.currentClientVisibleShapes[clientId]
        if (visibleShapes != null) {
            println("client $clientId visible shapes ---------------")
            val gameClient = world.getClient(clientId)!!
            val clientData = visibleShapes.joinToString(",") { vs ->
                // vs is in world coordinates, remove the client's top left corner position to get it relative to the client's real dimensions
                val adjustedToClientViewPosition = vs.position - gameClient.worldBounds.first

                // now scale down to the client's screen size
                val scaling = 1f * gameClient.screenSize.width / world.config.width
                val scaledToClientViewPosition = Point(
                    (adjustedToClientViewPosition.x * scaling).roundToInt(),
                    (adjustedToClientViewPosition.y * scaling).roundToInt()
                )
                println("scaled from $vs to $scaledToClientViewPosition")

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
package controller

import config.WorldConfiguration
import domain.World
import geometry.Point
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import kotlin.math.roundToInt

@Controller("/")
open class WorldController(
    private val world: World,
    private val config: WorldConfiguration
) {

    // s is "step" though it isn't used yet
    @OptIn(ExperimentalStdlibApi::class)
    @Get("w/{clientId}{?s}", produces = [MediaType.APPLICATION_OCTET_STREAM])
    fun getWorldData(clientId: Int, @QueryValue @Nullable s: Int?): HttpResponse<ByteArray> {
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
        println("client: $clientId, status: ${appStatus.toHexString()}")
        return HttpResponse.ok(byteArrayOf(stepNumber, appStatus) + data)
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

                // now scale down to the client's screen size, we need the width of the minor window view
                val scaling = 1f * gameClient.screenSize.width / config.width
                val scaledToClientViewPosition = Point(
                    (adjustedToClientViewPosition.x * scaling).roundToInt(),
                    (adjustedToClientViewPosition.y * scaling).roundToInt()
                )

                // now convert to a comma delimited string
                "${vs.shapeId},${scaledToClientViewPosition.x},${scaledToClientViewPosition.y}"
            }
            // prepend with the count of shapes we need to read from the string
            return "${visibleShapes.size},$clientData"
        } else {
            return ""
        }
    }

    private fun addWord(array: MutableList<Byte>, v: Int) {
        val vL = v and 255
        val vH = (v / 256) and 255
        array.add(vL.toByte())
        array.add(vH.toByte())
    }

    private fun addBool(array: MutableList<Byte>, v: Boolean) {
        val asInt = if (v) 1 else 0
        array.add(asInt.toByte())
    }

    @Get("ws", produces = [MediaType.APPLICATION_OCTET_STREAM])
    fun getWorldState(): HttpResponse<ByteArray> {
        val data = mutableListOf<Byte>()
        addWord(data, world.simulator.width)
        addWord(data, world.simulator.height)
        addWord(data, world.simulator.bodies.count())
        val bodiesByCount = world.simulator.bodies.groupingBy { (it.radius * 2).toInt() }.eachCount()
        data.add(bodiesByCount.getOrDefault(1, 0).toByte())
        data.add(bodiesByCount.getOrDefault(2, 0).toByte())
        data.add(bodiesByCount.getOrDefault(3, 0).toByte())
        data.add(bodiesByCount.getOrDefault(4, 0).toByte())
        data.add(bodiesByCount.getOrDefault(5, 0).toByte())
        data.add(world.clientHeartbeats.count().toByte())
        addBool(data, world.isFrozen)
        addBool(data, world.simulator.enableWrapping)
        return HttpResponse.ok(data.toByteArray())
    }

    @Get("freeze", produces = [MediaType.APPLICATION_OCTET_STREAM])
    fun toggleFreeze(): HttpResponse<ByteArray> {
        world.toggleFrozen();
        println("toggled frozen to ${world.isFrozen}")
        return HttpResponse.ok(byteArrayOf(if (world.isFrozen) 1 else 0))
    }
}
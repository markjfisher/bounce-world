package controller

import config.WorldConfiguration
import domain.World
import geometry.Point
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.APPLICATION_OCTET_STREAM
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import kotlin.math.roundToInt

@Controller("/")
open class WorldController(
    private val world: World,
    private val config: WorldConfiguration
) {

    @OptIn(ExperimentalStdlibApi::class)
    private val hexFormat = HexFormat {
        upperCase = false
        bytes {
            bytesPerGroup = 1
            groupSeparator = " "
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Get("w/{clientId}", produces = [APPLICATION_OCTET_STREAM])
//    fun getWorldData(clientId: Int, @QueryValue @Nullable s: Int?): HttpResponse<ByteArray> {
    fun getWorldData(clientId: Int): HttpResponse<ByteArray> {
        world.clientHeartbeats[clientId] = System.currentTimeMillis()
        val data = try {
            val csv = asCSV(clientId)
            // if there are no bodies in the view, we will return a value of 0 (as byte)
            if (csv.isNotEmpty()) csv.split(",").map { it.toInt().toByte() }.toByteArray() else byteArrayOf(0)
        } catch (e: Exception) {
            println("ERROR processing client ${clientId}: ${e.message}, sending 0")
            byteArrayOf(0)
        }
        val stepNumber = world.currentSimulator.currentStep.toByte()
        val appStatus = world.calculateStatus(clientId)
        val clientData = byteArrayOf(stepNumber, appStatus) + data
        // println("client: $clientId, data: ${clientData.toHexString(hexFormat)}")
        return HttpResponse.ok(clientData)
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

    @Get("ws", produces = [APPLICATION_OCTET_STREAM])
    fun getWorldState(): HttpResponse<ByteArray> {
        val data = mutableListOf<Byte>()
        addWord(data, world.currentSimulator.width)
        addWord(data, world.currentSimulator.height)
        addWord(data, world.currentSimulator.bodies.count())
        val bodiesByCount = world.currentSimulator.bodies.groupingBy { (it.radius * 2).toInt() }.eachCount()
        data.add(bodiesByCount.getOrDefault(1, 0).toByte())
        data.add(bodiesByCount.getOrDefault(2, 0).toByte())
        data.add(bodiesByCount.getOrDefault(3, 0).toByte())
        data.add(bodiesByCount.getOrDefault(4, 0).toByte())
        data.add(bodiesByCount.getOrDefault(5, 0).toByte())
        data.add(world.clientHeartbeats.count().toByte())
        addBool(data, world.isFrozen)
        addBool(data, world.isWrapping)
        return HttpResponse.ok(data.toByteArray())
    }

    // Technically should be a PUT, but it's much easier to hit the freeze endpoint this way for the client.
    @Get("freeze", produces = [APPLICATION_OCTET_STREAM])
    fun toggleFreeze(): HttpResponse<ByteArray> {
        world.toggleFrozen()
        println("toggled frozen to ${world.isFrozen}")
        return HttpResponse.ok(byteArrayOf(1))
    }

    @Get("add/{size}", produces = [APPLICATION_OCTET_STREAM])
    open fun addBody(size: Int): HttpResponse<ByteArray> {
        // we get a single byte for the size of shape we want to add
        println("asked to add body of size $size")
        if (size in 1..5) {
            println("Adding body size: $size")
            world.addBody(size)
        }
        return HttpResponse.ok(byteArrayOf(1))
    }

    @Get("reset", produces = [APPLICATION_OCTET_STREAM])
    fun resetWorld(): HttpResponse<ByteArray> {
        world.resetWorld()
        println("world reset!")
        return HttpResponse.ok(byteArrayOf(1))
    }

    @Get("inc", produces = [APPLICATION_OCTET_STREAM])
    fun increaseSpeed(): HttpResponse<ByteArray> {
        world.increaseSpeed()
        println("increasing speed")
        return HttpResponse.ok(byteArrayOf(1))
    }

    @Get("dec", produces = [APPLICATION_OCTET_STREAM])
    fun decreaseSpeed(): HttpResponse<ByteArray> {
        world.decreaseSpeed()
        println("decreasing speed")
        return HttpResponse.ok(byteArrayOf(1))
    }


}
package controller

import config.WorldConfiguration
import domain.BodyData
import domain.BodySummary
import domain.ClientBasic
import domain.ClientCommand
import domain.ClientData
import domain.VectorData
import domain.World
import domain.WorldStatus
import geometry.Point
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.MediaType.APPLICATION_OCTET_STREAM
import io.micronaut.http.MediaType.TEXT_PLAIN
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
        if (!world.clients().map { it.id }.contains(clientId)) {
            return HttpResponse.ok(byteArrayOf(0))
        }
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
                val scalingX = 1f * gameClient.screenSize.width / config.width
                val scalingY = 1f * gameClient.screenSize.height / config.height
                val scaledToClientViewPosition = Point(
                    (adjustedToClientViewPosition.x * scalingX).roundToInt(),
                    (adjustedToClientViewPosition.y * scalingY).roundToInt()
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

    @Get("status", produces = [APPLICATION_JSON])
    fun getState(): HttpResponse<WorldStatus> {
        val bodyGrouping = world.currentSimulator.bodies.groupingBy { (it.radius * 2).toInt() }.eachCount()
        val worldStatus = WorldStatus(
            width = world.currentSimulator.width,
            height = world.currentSimulator.height,
            frozen = world.isFrozen,
            wrapping = world.isWrapping,
            clients = world.clients().map { ClientData(id = it.id, name = it.name, location = it.position) },
            bodyCounts = bodyGrouping.map { (size, count) -> BodySummary(size, count) },
            bodies = world.currentSimulator.bodies.map { BodyData(id = it.id, radius = it.radius, mass = it.mass, position = VectorData(it.position.x, it.position.y), velocity = VectorData(it.velocity.x, it.position.y)) },
        )
        return HttpResponse.ok(worldStatus)
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

    @Get("who", produces = [TEXT_PLAIN])
    fun who(): HttpResponse<String> {
        val fixedString = world.clients().sortedBy { it.id }.map { it.name }.joinToString(separator = "") { it.padEnd(8, ' ') }
        return HttpResponse.ok(fixedString)
    }

    // This puts commands onto the client's 'queue' to process, typically called from an external application, not the clients
    // I'm not following the standard VERBS here for mega simplicity for clients
    @Get("cmd/put/{clientId}/{cmd}", produces = [APPLICATION_OCTET_STREAM])
    fun clientCommand(clientId: String, cmd: String): HttpResponse<ByteArray> {
        val clientCommand = ClientCommand.from(cmd) ?: return HttpResponse.notFound(byteArrayOf())

        if (clientId == "ALL") {
            world.addCommandToAllClients(clientCommand)
        } else {
            val id = clientId.toIntOrNull() ?: return HttpResponse.notFound(byteArrayOf())
            val client = world.getClient(id) ?: return HttpResponse.notFound(byteArrayOf())
            world.addCommandToClient(client.id, clientCommand)
        }

        return HttpResponse.ok(byteArrayOf(1))
    }

    // This fetches the cmd code bytes the client has been instructed to perform
    @Get("cmd/get/{clientId}", produces = [APPLICATION_OCTET_STREAM])
    fun fetchCommands(clientId: String): HttpResponse<ByteArray> {
        val id = clientId.toIntOrNull() ?: return HttpResponse.ok(byteArrayOf())
        val client = world.getClient(id) ?: return HttpResponse.ok(byteArrayOf())
        val commandData = world.getCommands(client.id)
        return HttpResponse.ok(commandData)
    }

    @Get("cmd/broadcast/{clientId}/{time}/{message}", produces = [APPLICATION_OCTET_STREAM])
    fun broadcastCommand(clientId: String, time: String, message: String): HttpResponse<ByteArray> {
        val t = time.toIntOrNull() ?: return HttpResponse.notFound(byteArrayOf())
        if (clientId == "ALL") {
            world.broadcastToAllClients(message, t)
        } else {
            val id = clientId.toIntOrNull() ?: return HttpResponse.notFound(byteArrayOf())
            world.broadcastToClient(id, message, t)
        }
        return HttpResponse.ok(byteArrayOf(1))
    }

    // this retrieves the latest broadcast message
    @Get("msg", produces = [TEXT_PLAIN])
    fun msg(): HttpResponse<String> {
        return HttpResponse.ok(world.currentBroadcastMessage)
    }

    @Get("get-clients", produces = [APPLICATION_JSON])
    fun getClients(): HttpResponse<List<ClientBasic>> {
        return HttpResponse.ok(world.clients().map { ClientBasic(it.id, it.name) })
    }

    @Get("reorder/{clientOrderCS}", produces = [APPLICATION_OCTET_STREAM])
    fun reorderClients(clientOrderCS: String): HttpResponse<String> {
        println("reorder, ids: $clientOrderCS")
        val newClientOrder = clientOrderCS.split(",").map { it.toInt() }
        val newIds = newClientOrder.toSortedSet()

        val oldClients = world.clients()
        val oldIds = oldClients.map { it.id }.toSortedSet()

        if (oldIds != newIds) {
            println("Error: old and new ids do not match, oldIds: $oldIds, newIds: $newIds")
            return HttpResponse.badRequest("New IDs do not match current client IDs, oldIds: $oldIds, newIds: $newIds")
        }

        world.rebuild(newClientOrder)
        return HttpResponse.ok("")
    }
}
package command

import config.WorldConfig
import domain.BodyData
import domain.BodySummary
import domain.ClientBasic
import domain.ClientCommand
import domain.ClientData
import domain.VectorData
import domain.World
import domain.WorldStatus
import geometry.Point
import logger
import kotlin.math.roundToInt

class WorldCommandProcessor(private val world: World, private val config: WorldConfig) {
    fun getWorldData(id: Int): ByteArray {
        if (!world.clientIds().contains(id)) {
            return byteArrayOf(0)
        }
        world.updateHeartbeat(id)
        val data = try {
            val csv = asCSV(id)
            // if there are no bodies in the view, we will return a value of 0 (as byte)
            if (csv.isNotEmpty()) {
                csv.split(",").map { it.toInt().toByte() }.toByteArray()
            } else {
                byteArrayOf(0)
            }
        } catch (e: Exception) {
            logger.error("ERROR processing client ${id}: ${e.message}, sending 0")
            byteArrayOf(0)
        }

        val stepNumber = world.currentSimulator.currentStep.toByte()
        val appStatus = world.calculateStatus(id)
        val clientData = byteArrayOf(stepNumber, appStatus) + data
        return clientData
    }

    fun getWorldState(): ByteArray {
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
        return data.toByteArray()
    }

    fun getStatus(): WorldStatus {
        val bodyGrouping = world.currentSimulator.bodies.groupingBy { (it.radius * 2).toInt() }.eachCount()
        val worldStatus = WorldStatus(
            width = world.currentSimulator.width,
            height = world.currentSimulator.height,
            frozen = world.isFrozen,
            wrapping = world.isWrapping,
            clients = world.clients().map { ClientData(id = it.id, name = it.name, location = it.position) },
            bodyCounts = bodyGrouping.map { (size, count) -> BodySummary(size, count) },
            bodies = world.currentSimulator.bodies.map {
                BodyData(
                    id = it.id,
                    radius = it.radius,
                    mass = it.mass,
                    position = VectorData(it.position.x, it.position.y),
                    velocity = VectorData(it.velocity.x, it.position.y)
                )
            }
        )

        return worldStatus

    }

    fun toggleFreeze(): ByteArray {
        world.toggleFrozen()
        return byteArrayOf(1) // Success response
    }

    fun resetWorld(): ByteArray {
        world.resetWorld()
        return byteArrayOf(1) // Success response
    }

    fun increaseSpeed(): ByteArray {
        world.increaseSpeed()
        return byteArrayOf(1) // Success response
    }

    fun decreaseSpeed(): ByteArray {
        world.decreaseSpeed()
        return byteArrayOf(1) // Success response
    }

    fun addRandomBodyWithSize(size: Int): ByteArray {
        if (size in 1..5) {
            world.addRandomBodyWithSize(size)
        }
        return byteArrayOf(1) // Success response
    }

    fun addBody(shapeId: Int, clientId: Int): ByteArray {
        val client = world.getClient(clientId) ?: return byteArrayOf(0)
        world.getShapes().firstOrNull { it.id == shapeId } ?: return byteArrayOf(0)

        world.addBody(shapeId, client.position)
        return byteArrayOf(1)
    }

    fun clientCommand(clientId: String, cmd: String): ByteArray {
        val clientCommand = ClientCommand.from(cmd) ?: return byteArrayOf(0)

        if (clientId == "ALL") {
            world.addCommandToAllClients(clientCommand)
        } else {
            val id = clientId.toIntOrNull() ?: return byteArrayOf(0)
            val client = world.getClient(id) ?: return byteArrayOf(0)
            world.addCommandToClient(client.id, clientCommand)
        }
        return byteArrayOf(1)
    }

    fun broadcastCommand(clientId: String, time: String, message: String): ByteArray {
        val t = time.toIntOrNull() ?: return byteArrayOf(0)
        if (clientId == "ALL") {
            world.broadcastToAllClients(message, t)
        } else {
            val id = clientId.toIntOrNull() ?: return byteArrayOf(0)
            world.broadcastToClient(id, message, t)
        }

        return byteArrayOf(1)
    }

     // This fetches the cmd code bytes the client has been instructed to perform
     fun fetchCommands(clientId: String): ByteArray {
         val id = clientId.toIntOrNull() ?: return byteArrayOf(0)
         val client = world.getClient(id) ?: return byteArrayOf(0)
         val commandData = world.getCommands(client.id)
         return commandData
     }

    fun getLatestMessage(): String {
        return world.currentBroadcastMessage
    }

    fun getClients(): List<ClientBasic> {
        return world.clients().map { ClientBasic(it.id, it.name) }
    }

    fun who(): String {
        val fixedString = world.clients().sortedBy { it.id }.map { it.name }.joinToString(separator = "") { it.padEnd(8, ' ') }
        return fixedString
    }

    fun close(clientId: String): ByteArray {
        // logger.info("closing client $clientId")
        val id = clientId.toIntOrNull()
        if (id != null) {
            world.removeClient(id)
        }
        return byteArrayOf(1)
    }

    private fun asCSV(clientId: Int): String {
        val visibleShapes = world.currentClientVisibleShapes[clientId]
        if (!visibleShapes.isNullOrEmpty()) {
            // println("client $clientId visible shapes ---------------")
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
}
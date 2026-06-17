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
import logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class WorldCommandProcessor(private val world: World, private val config: WorldConfig) {
    fun getWorldData(id: Int): ByteArray {
        if (!world.clientIds().contains(id)) {
            return byteArrayOf(0)
        }
        world.updateHeartbeat(id)
        val data = asBinary(id)
        val stepNumber = world.currentSimulator.currentStep.toByte()
        val appStatus = world.calculateStatus(id)
        val clientData = byteArrayOf(stepNumber, appStatus) + data
        return clientData
    }

    fun getWorldDataWithSize(id: Int): ByteArray = prependPacketSize(getWorldData(id))

    companion object {
        private const val PACKET_SIZE_BYTES = 2

        fun prependPacketSize(payload: ByteArray): ByteArray {
            val totalSize = PACKET_SIZE_BYTES + payload.size
            return byteArrayOf(
                (totalSize and 0xFF).toByte(),
                ((totalSize shr 8) and 0xFF).toByte(),
            ) + payload
        }
    }

    fun getWorldState(): ByteArray {
        val data = mutableListOf<Byte>()
        addWord(data, world.currentSimulator.width)
        addWord(data, world.currentSimulator.height)
        addWord(data, world.currentSimulator.bodyCount())
        val bodiesByCount = world.currentSimulator.groupingBodiesBy { (it.radius * 2).toInt() }.eachCount()
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
        val bodyGrouping = world.currentSimulator.groupingBodiesBy { (it.radius * 2).toInt() }.eachCount()
        val worldStatus = WorldStatus(
            width = world.currentSimulator.width,
            height = world.currentSimulator.height,
            frozen = world.isFrozen,
            wrapping = world.isWrapping,
            clients = world.clients().map { ClientData(id = it.id, name = it.name, location = it.position) },
            bodyCounts = bodyGrouping.map { (size, count) -> BodySummary(size, count) },
            bodies = world.currentSimulator.mapBodies {
                BodyData(
                    id = it.id,
                    radius = it.radius,
                    mass = it.mass,
                    position = VectorData(it.position.x, it.position.y),
                    velocity = VectorData(it.velocity.x, it.velocity.y)
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
        world.shapes.firstOrNull { it.id == shapeId } ?: return byteArrayOf(0)

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

    fun asBinary(clientId: Int): ByteArray {
        return try {
            val visibleShapes = world.currentClientVisibleShapes[clientId]
            if (visibleShapes.isNullOrEmpty()) {
                byteArrayOf(0)
            } else {
                val gameClient = world.getClient(clientId)!!
                val scaleX = gameClient.screenSize.width.toFloat() / config.width
                val scaleY = gameClient.screenSize.height.toFloat() / config.height

                // Cap to 240 shapes to keep count within 1 byte
                val capped = visibleShapes.take(240)
                val count = capped.size

                // Layout: [count:byte] then for each shape [shapeId:byte][x:byte][y:byte]
                val capacity = 1 + count * 3
                val buf = ByteBuffer
                    .allocate(capacity)
                    .order(ByteOrder.LITTLE_ENDIAN) // choose and stick to an endianness

                // write the count of shapes - THIS COULD BE A SHORT IF WE EXTEND TO > 255
                // but would need to adjust the capacity above
                // buf.putShort(count.toShort())
                buf.put(count.toByte())

                for (vs in capped) {
                    val adjusted = vs.position - gameClient.worldBounds.first
                    val sx = (adjusted.x * scaleX).roundToInt().toByte()
                    val sy = (adjusted.y * scaleY).roundToInt().toByte()

                    buf.put(vs.shapeId.toByte())
                    buf.put(sx)
                    buf.put(sy)
                }

                buf.array()
            }
        } catch (e: Exception) {
            logger.error("ERROR processing client $clientId: ${e.message}, sending 0")
            byteArrayOf(0)
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
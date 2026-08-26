package command

import config.WorldConfig
import domain.BodyData
import domain.BodySummary
import domain.ClientBasic
import domain.ClientCapabilities
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
    companion object {
        private const val TAU = (2 * Math.PI).toFloat()
        // angle wire encoding: uint16 across a full turn, 65535 = 2pi
        const val MAX_ANGLE_BITS = 65535f
        // angular velocity wire encoding: int16 fixed point, units of 1/256 rad/s
        const val ANGULAR_VELOCITY_SCALE = 256f
    }

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
                val scaleX = gameClient.screenSize.width.toFloat() / gameClient.region.width
                val scaleY = gameClient.screenSize.height.toFloat() / gameClient.region.height

                // A MutableSet deliberately removes duplicate copies, but its iteration
                // order is not a wire contract. Stabilise packets for reproducible
                // clients/tests; body id is the logical identity and x/y order copies.
                val capped = visibleShapes
                    .sortedWith(compareBy<domain.VisibleShape> { it.bodyId }
                        .thenBy { it.position.x }
                        .thenBy { it.position.y }
                        .thenBy { it.shapeId })
                    .take(240)
                val count = capped.size
                val wideCoords = ClientCapabilities.has(gameClient.capabilities, ClientCapabilities.WIDE_COORDS)
                val rotation = ClientCapabilities.has(gameClient.capabilities, ClientCapabilities.ROTATION)
                val bodyId = ClientCapabilities.has(gameClient.capabilities, ClientCapabilities.BODY_ID)
                val coordBytes = if (wideCoords) 2 else 1

                // Layout: [count:byte] then for each shape [shapeId:byte][x][y] plus optional extras:
                //   wide coords: x/y are 2 bytes (LE short) instead of 1 byte
                //   rotation:    [angle: uint16 LE][angularVelocity: int16 LE] appended after coordinates;
                //                angle is scaled across a full turn (65535 = 2pi), omega is 1/256 rad/s
                //   body id:     [bodyId: uint32 LE] appended after all pre-existing optional fields.
                val shapeBytes = 1 + coordBytes * 2 +
                    (if (rotation) 4 else 0) + (if (bodyId) 4 else 0)
                val capacity = 1 + count * shapeBytes
                val buf = ByteBuffer
                    .allocate(capacity)
                    .order(ByteOrder.LITTLE_ENDIAN) // choose and stick to an endianness

                // write the count of shapes - THIS COULD BE A SHORT IF WE EXTEND TO > 255
                // but would need to adjust the capacity above
                // buf.putShort(count.toShort())
                buf.put(count.toByte())

                for (vs in capped) {
                    // translate into the client's region, scale float world units to client pixels,
                    // and only round once, at the final pixel step
                    val adjustedX = vs.position.x - gameClient.region.upperLeft.x
                    val adjustedY = vs.position.y - gameClient.region.upperLeft.y
                    val sx = (adjustedX * scaleX).roundToInt()
                    val sy = (adjustedY * scaleY).roundToInt()

                    buf.put(vs.shapeId.toByte())
                    if (wideCoords) {
                        buf.putShort(sx.toShort())
                        buf.putShort(sy.toShort())
                    } else {
                        buf.put(sx.toByte())
                        buf.put(sy.toByte())
                    }
                    if (rotation) {
                        val angleBits = (((vs.angle % TAU + TAU) % TAU) / TAU * MAX_ANGLE_BITS).roundToInt()
                        buf.putShort(angleBits.coerceIn(0, MAX_ANGLE_BITS.toInt()).toShort())
                        val omegaBits = (vs.angularVelocity * ANGULAR_VELOCITY_SCALE).roundToInt()
                        buf.putShort(omegaBits.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
                    }
                    if (bodyId) {
                        buf.putInt(vs.bodyId)
                    }
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

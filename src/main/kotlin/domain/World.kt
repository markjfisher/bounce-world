package domain

import config.WorldConfiguration
import data.ShapeCreator
import geometry.GridPatternGenerator
import geometry.Point
import geometry.bounds
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joml.Vector2f
import simulator.WorldSimulator
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

@Singleton
open class World(
    private val config: WorldConfiguration
) {
    private val simulationScope = CoroutineScope(Dispatchers.Default)
    private val heartbeatScope = CoroutineScope(Dispatchers.IO)

    // the data about clients
    private val clients = mutableMapOf<String, GameClient>()

    // the client streams
    private val clientChannels = mutableMapOf<String, Channel<ByteArray>>()

    // last heartbeat received
    val clientHeartbeats = mutableMapOf<String, Long>()

    // which positions in the spiral pattern are currently taken
    private val occupiedScreens = mutableMapOf<Point, GameClient>()

    // the body shapes the clients will be told about
    val shapes = ShapeCreator.createShapes()

    private var isStarted = false
    private var stopped = false

    private val currentClientVisibleShapes = mutableMapOf<String, MutableSet<VisibleShape>>()

    val simulator = WorldSimulator(width = config.width, height = config.height, scalingFactor = config.scalingFactor)

    fun setDelay(delay: Long) {
        config.stepDelayMillis = delay
    }

    init {
//        val newBodies = createBodies(0, 0, 0, listOf(5, 3, 3, 2, 2, 2, 1, 1, 1, 1))
//        val newBodies = createBodies(0, 0, 0, listOf(5, 3, 3, 2, 2, 2))
        val newBodies = createBodies(0,0, 0, listOf(5, 5))
//        val newBodies = createBodies(0,0, 0, List(30) { 1 } + List(10) { 2 } + List(5) { 3 } + List(2) { 5 })
        simulator.addBodies(newBodies)
    }

    private suspend fun checkClientsStillConnected() {
        while (!stopped) {
            val clientIds = clientChannels.keys.toList() // stops the concurrent update error as we're not modifying this list, just the clientChannels
            clientIds.forEach { clientId ->
                val sinceHeartbeat = System.currentTimeMillis() - clientHeartbeats[clientId]!!
                if (sinceHeartbeat > config.heartbeatTimeoutMillis) {
                    println("No heartbeat from client ${clients[clientId]!!.name} for $sinceHeartbeat ms, killing it.")
                    unregisterClient(clientId)
                }
            }
            delay(5000L)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun runSimulation() {
        isStarted = true
        while (!stopped) {
            simulator.step()
            currentClientVisibleShapes.clear()
            currentClientVisibleShapes.putAll(simulator.findVisibleShapesByClient(clients.values.toList()))

            clientChannels.forEach { (clientId, channel) ->
                val data = try {
                    val csv = asCSV(clientId)
                    println("sending ${clients[clientId]!!.name}: $csv")
                    // if there are no bodies in the view, we will return a value of "0"
                    if (csv.isNotEmpty()) csv.split(",").map { it.toInt().toByte() }.toByteArray() else byteArrayOf(0)
                } catch (e: Exception) {
                    println("ERROR processing client ${clientId}: ${e.message}, sending 0")
                    byteArrayOf(0)
                }
                try {
                    if (!channel.isClosedForSend) {
                        val stepNumber = simulator.currentStep.toByte()
                        channel.send(byteArrayOf(stepNumber) + data)
                    }
                } catch (e: ClosedSendChannelException) {
                    println("channel closed for client $clientId, removing it.")
                    unregisterClient(clientId)
                }
            }

            delay(config.stepDelayMillis)
        }
    }

    private fun unregisterClient(clientId: String) {
        clientChannels.remove(clientId)?.close()
        removeClient(clientId)
    }

    open fun addClient(gameClient: GameClient) {
        val nextPoint = findNextUnoccupiedScreen()
        gameClient.position = nextPoint
        clients[gameClient.id] = gameClient
        occupiedScreens[nextPoint] = gameClient

        // adjust the simulator's dimensions
        simulator.width = worldBoundary().x * config.width
        simulator.height = worldBoundary().y * config.height

        if (!isStarted && config.shouldAutoStart) {
            simulationScope.launch {
                runSimulation()
            }
            heartbeatScope.launch {
                checkClientsStillConnected()
            }
        }
    }

    fun createClient(gameClientInfo: GameClientInfo): GameClient {
        val client = GameClient(
            id = UUID.randomUUID().toString().substring(0, 8),
            name = gameClientInfo.name,
            version = gameClientInfo.version,
            screenSize = gameClientInfo.screenSize
        )
        addClient(client)
        client.updateWorldBounds(config.width, config.height)
        return client
    }

    fun registerClientChannel(clientId: String): Channel<ByteArray> {
        val channel = Channel<ByteArray>(Channel.CONFLATED)
        clientChannels[clientId] = channel
        clientHeartbeats[clientId] = System.currentTimeMillis()
        return channel
    }

    fun at(point: Point): GameClient? = occupiedScreens[point]
    fun getClient(id: String): GameClient? = clients[id]

    private fun createRandomBodyWithShape(id: Int, shapeId: Int, offsetX: Int, offsetY: Int): Body {
        val shape = shapes.first { it.id == shapeId }
        return Body.from(
            id = id,
            // position is a random location: [(offsetX to offsetX + screenWidth), (offsetY to offsetY + screenHeight)]
            position = Vector2f(
                Random.nextFloat() * config.width + offsetX,
                Random.nextFloat() * config.height + offsetY
            ),
            // everything starts with speed "initialSpeed" pixels/sec in some random direction
            velocity = Vector2f(
                Random.nextFloat() * 2f - 1f,
                Random.nextFloat() * 2f - 1f
            ).normalize().mul(config.initialSpeed),
            shape = shape
        )
    }

    // generates new bodies within a screen, adding the offsets given to position, the world simulator will fit them as close as it can to their position
    private fun createBodies(startId: Int, offsetX: Int, offsetY: Int, sizes: List<Int>): List<Body> = sizes.mapIndexed { i, size ->
        // find a random shape with the given size and create a body from it
        createRandomBodyWithShape(startId + i, shapes.groupBy { it.sideLength }[size]!!.random().id, offsetX, offsetY)
    }

    private fun findNextUnoccupiedScreen(): Point {
        // walk the sequence of grid points until we find one not in occupiedPoints.
        // This will allow clients to be removed from the world, and replaced by new joiners
        val spiralPoints = GridPatternGenerator().generate().iterator()
        while (spiralPoints.hasNext()) {
            val point = spiralPoints.next()
            if (!occupiedScreens.containsKey(point)) {
                return point
            }
        }
        throw IllegalStateException("Unable to find next unoccupied point.")
    }

    fun removeClient(id: String) {
        val client = getClient(id) ?: return
        clients.remove(client.id)
        val entriesForClient = occupiedScreens.filterValues { c -> c.id == id }
        if (entriesForClient.isNotEmpty()) {
            occupiedScreens.remove(entriesForClient.keys.first())
        }
    }

    fun worldBoundary(): Point {
        if (occupiedScreens.isEmpty()) return Point(1, 1)
        return occupiedScreens.keys.bounds().second + Point(1, 1)
    }

    companion object {
        // screens of 40x20 and 32x16 have good mappings down from these sizes
        // The world size will be its boundary * these values.
        const val SCREEN_WIDTH = 160
        const val SCREEN_HEIGHT = 80
    }

    private fun asCSV(clientId: String): String {
        val visibleShapes = currentClientVisibleShapes[clientId]
        if (!visibleShapes.isNullOrEmpty()) {
//            println("client $clientId visible shapes ---------------")
            val gameClient = getClient(clientId)!!
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
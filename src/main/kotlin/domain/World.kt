package domain

import config.WorldConfiguration
import data.ShapeCreator
import geometry.GridPatternGenerator
import geometry.LocationGenerator
import geometry.Point
import geometry.RightGenerator
import geometry.bounds
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joml.Vector2f
import simulator.WorldSimulator
import kotlin.random.Random

@Singleton
open class World(
    private val config: WorldConfiguration,
    val simulator: WorldSimulator,
) {
    private val simulationScope = CoroutineScope(Dispatchers.Default)
    private val heartbeatScope = CoroutineScope(Dispatchers.IO)

    // the data about clients
    private val clients = mutableMapOf<Int, GameClient>()

    // last heartbeat received
    val clientHeartbeats = mutableMapOf<Int, Long>()

    // status events the client needs to be told about
    private val statusEvents = mutableMapOf<Int, MutableSet<StatusEvent>>()

    // which positions in the location pattern are currently taken
    private val occupiedScreens = mutableMapOf<Point, GameClient>()

    // the body shapes the clients will be told about
    val shapes = ShapeCreator.createShapes()

    private var isStarted = false
    var isFrozen = false
    private var stopped = false
    private var nextClientId = 0
    private val stepTime = 1f / config.updatesPerSecond

    val currentClientVisibleShapes = mutableMapOf<Int, MutableSet<VisibleShape>>()

    init {
//        val newBodies = createBodies(0, 0, 0, listOf(5, 3))
        val newBodies = createBodies(0,0, 0, List(5) { 2 } + List(3) { 3 } + List(1) { 5 })
        simulator.addBodies(newBodies)
    }

    private suspend fun checkClientsStillConnected() {
        while (!stopped) {
            // creating a list to iterate over instead of directly on the keys stops the concurrent update error as we're not modifying this list
            val clientIds = clients.keys.toList() // use the clients directly rather than channels, as they may not have sent any data yet, so aren't in the channels list, but we do have them in the initial heartbeats
            clientIds.forEach { clientId ->
                val sinceHeartbeat = System.currentTimeMillis() - (clientHeartbeats[clientId] ?: 0)
                if (sinceHeartbeat > config.heartbeatTimeoutMillis) {
                    println("No heartbeat from client ${clients[clientId]?.name ?: "UNKNOWN"} for $sinceHeartbeat ms, unregistering client.")
                    unregisterClient(clientId)
                }
            }
            delay(5000L)
        }
    }

    private suspend fun runSimulation() {
        var started = 0L
        isStarted = true
        while (!stopped) {
            started = System.currentTimeMillis()
            if (!isFrozen) {
                // TODO: should we remove all collision events that haven't been read by clients, other event types will stick around until client has read them, but collisions should not be sticky
                simulator.step()
                currentClientVisibleShapes.clear()
                currentClientVisibleShapes.putAll(simulator.findVisibleShapesByClient(clients.values.toList()))
                // find all the clients with the collisions this step so we can add a collision event
                // we have body1 body2 in collisions, and those ids are in the visibleShapes of a client
                clients.keys.forEach { clientId ->
                    val bodyIdsForCurrentClient = currentClientVisibleShapes[clientId]?.map { it.bodyId }?.toSet() ?: setOf()
                    if (bodyIdsForCurrentClient.intersect(simulator.collisions).isNotEmpty()) {
                        // this client has a body in its view that had a collision this step
                        addEvent(clientId, StatusEvent.COLLISION)
                    }
                }
            } else {
                addEventToAllClients(StatusEvent.FROZEN_TOGGLE)
            }

            val timeTaken = (System.currentTimeMillis() - started) / 1000f
            if (timeTaken < stepTime) {
                val d = (stepTime - timeTaken) * 1000f
                delay(d.toLong())
            }

        }
    }

    private fun unregisterClient(clientId: Int) {
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
            id = nextClientId++,
            name = gameClientInfo.name,
            version = gameClientInfo.version,
            screenSize = gameClientInfo.screenSize
        )
        addClient(client)
        client.updateWorldBounds(config.width, config.height)
        clientHeartbeats[client.id] = System.currentTimeMillis()
        return client
    }

    fun at(point: Point): GameClient? = occupiedScreens[point]
    fun getClient(id: Int): GameClient? = clients[id]

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
        // walk the sequence of next location points until we find one not in occupiedPoints.
        // This will allow clients to be removed from the world, and replaced by new joiners
        val pointGenerator: LocationGenerator = when(config.locationPattern) {
            "grid" -> GridPatternGenerator()
            "right" -> RightGenerator()
            else -> throw Error("Unknown location pattern ${config.locationPattern}")
        }

        val pointIterator = pointGenerator.generate().iterator()
        while (pointIterator.hasNext()) {
            val point = pointIterator.next()
            if (!occupiedScreens.containsKey(point)) {
                return point
            }
        }
        throw IllegalStateException("Unable to find next unoccupied point.")
    }

    fun removeClient(id: Int) {
        val client = getClient(id) ?: return
        clients.remove(client.id)
        clientHeartbeats.remove(client.id)
        statusEvents.remove(client.id)
        val entriesForClient = occupiedScreens.filterValues { c -> c.id == id }
        if (entriesForClient.isNotEmpty()) {
            occupiedScreens.remove(entriesForClient.keys.first())
        }
    }

    fun worldBoundary(): Point {
        if (occupiedScreens.isEmpty()) return Point(1, 1)
        return occupiedScreens.keys.bounds().second + Point(1, 1)
    }

    fun addEventToAllClients(statusEvent: StatusEvent) {
        clients.keys.forEach { id ->
            val clientEvents = statusEvents[id] ?: mutableSetOf()
            clientEvents.add(statusEvent)
            statusEvents[id] = clientEvents
        }
    }

    fun removeEventFromAllClients(statusEvent: StatusEvent) {
        clients.keys.forEach { id ->
            val clientEvents = statusEvents[id] ?: mutableSetOf()
            clientEvents.remove(statusEvent)
        }
    }

    private fun addEvent(clientId: Int, statusEvent: StatusEvent) {
        val clientEvents = statusEvents[clientId] ?: mutableSetOf()
        clientEvents.add(statusEvent)
        statusEvents[clientId] = clientEvents
    }

    fun calculateStatus(clientId: Int): Byte {
        // if there are no events, status is 0
        val events = statusEvents[clientId] ?: return 0

        // remove the statuses from our map, as they are only sent once to the client
        statusEvents.remove(clientId)

        // add all the status values together to form the byte.
        // Each status value is a power of 2 (i.e. an individual bit) to make it easy for the client to determine values to react to
        return events.fold(0) { ac, e -> ac + e.value }.toByte()
    }

    fun toggleFrozen() {
        isFrozen = !isFrozen
        if (isFrozen) {
            addEventToAllClients(StatusEvent.FROZEN_TOGGLE)
        } else {
            removeEventFromAllClients(StatusEvent.FROZEN_TOGGLE)
        }
    }

    companion object {
        // screens of 40x20 and 32x16 have good mappings down from these sizes
        // The world size will be its boundary * these values.
        const val SCREEN_WIDTH = 160
        const val SCREEN_HEIGHT = 80
    }
}
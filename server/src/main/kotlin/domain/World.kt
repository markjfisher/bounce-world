package domain

import config.WorldConfig
import data.ShapeCreator
import geometry.GridPatternGenerator
import geometry.LocationGenerator
import geometry.Point
import geometry.RightGenerator
import geometry.bounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joml.Vector2f
import simulator.WorldSimulator
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

open class World(
    private val config: WorldConfig,
    private val wrappedSimulator: WorldSimulator,
    private val boundedSimulator: WorldSimulator
) {
    private val simulationScope = CoroutineScope(Dispatchers.Default)
    private val heartbeatScope = CoroutineScope(Dispatchers.IO)

    // the data about clients
    private val clients = mutableMapOf<Int, GameClient>()
    fun clients(): List<GameClient> = clients.values.toList()
    fun setClients(newClients: Map<Int, GameClient>) {
        clients.clear()
        clients.putAll(newClients)
    }

    // last heartbeat received
    val clientHeartbeats = mutableMapOf<Int, Long>()

    // status events the client needs to be told about
    private val statusEvents = mutableMapOf<Int, MutableSet<StatusEvent>>()

    // commands the client needs to be told about
    private val clientCommands = mutableMapOf<Int, MutableList<ClientCommand>>()

    // we only keep 1 broadcast message
    var currentBroadcastMessage: String = ""

    // which positions in the location pattern are currently taken
    private val occupiedScreens = mutableMapOf<Point, GameClient>()

    // the body shapes the clients will be told about
    val shapes = ShapeCreator.createShapes()

    // the current client location pattern
    private var currentLocationPattern = config.locationPattern

    private var isStarted = false
    var isFrozen = false
    private var stopped = false
    private var nextClientId = 0
    private val stepTime = 1f / config.updatesPerSecond
    var isWrapping = config.enableWrapping

    val currentSimulator: WorldSimulator
        get() = if (isWrapping) wrappedSimulator else boundedSimulator

    val currentClientVisibleShapes = mutableMapOf<Int, MutableSet<VisibleShape>>()

    fun rebuild(newIds: List<Int>) {
        isFrozen = true
        stopped = true
        currentSimulator.reset()
        occupiedScreens.clear()
        newIds.forEach { id ->
            // re-add all the clients, their new positions will be calculated as they are re-add and the world shape will regenerate based on the total new clients.
            val client = clients[id] ?: throw Exception("No client with id $id")
            addClient(client)
        }

        addEventToAllClients(StatusEvent.OBJECT_CHANGE)
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
        var started: Long
        isStarted = true

        while (!stopped) {
            started = System.currentTimeMillis()
            if (!isFrozen) {
                currentSimulator.step()
                currentClientVisibleShapes.clear()
                currentClientVisibleShapes.putAll(findVisibleShapesByClient())
                // find all the clients with the collisions this step so we can add a collision event
                // we have body1 body2 in collisions, and those ids are in the visibleShapes of a client
                clients.keys.forEach { clientId ->
                    val bodyIdsForCurrentClient = currentClientVisibleShapes[clientId]?.map { it.bodyId }?.toSet() ?: setOf()
                    if (bodyIdsForCurrentClient.intersect(currentSimulator.collisions).isNotEmpty()) {
                        // this client has a body in its view that had a collision this step
                        addEvent(clientId, StatusEvent.COLLISION)
                    }
                }
            } else {
                addEventToAllClients(StatusEvent.FROZEN)
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
        currentSimulator.width = worldBoundary().x * config.width
        currentSimulator.height = worldBoundary().y * config.height

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
        addEventToAllClients(StatusEvent.CLIENT_CHANGE)
        return client
    }

    fun at(point: Point): GameClient? = occupiedScreens[point]
    fun getClient(id: Int): GameClient? = clients[id]

    private fun createRandomBodyWithShape(id: Int, shapeId: Int, offsetX: Int, offsetY: Int): Body {
        val shape = shapes.first { it.id == shapeId }
        // Create a position that's within a screen's boundaries, but will be inside the particular client's boundary that created it.
        // This doesn't use the whole world width/height, just a single screen, and caters for the radius of the shape by reducing the possible x/y coordinates it start at to be within a screen's size
        val pos = Vector2f(
            Random.nextFloat() * (config.width.toFloat() - shape.sideLength - 5f) + offsetX + shape.sideLength / 2f + 2,
            Random.nextFloat() * (config.height.toFloat() - shape.sideLength - 5f) + offsetY + shape.sideLength / 2f + 2
        )
        var vx = Random.nextFloat() * 2f - 1f
        val vy = Random.nextFloat() * 2f - 1f
        if (abs(vx) < 0.000001f && abs(vy) < 0.000001f) {
            // ensure it's impossible to get a zero vector for velocity
            vx = 0.1f
        }
        return Body.from(
            id = id,
            // position is a random location: [(offsetX to offsetX + screenWidth), (offsetY to offsetY + screenHeight)]
            position = pos,
            // everything starts with speed "initialSpeed" pixels/sec in some random direction
            velocity = Vector2f(vx, vy).normalize().mul(config.initialSpeed),
            shape = shape
        )
    }

    // generates new bodies within a screen, adding the offsets given to position, the world simulator will fit them as close as it can to their position
    fun createBodies(startId: Int, offsetX: Int, offsetY: Int, sizes: List<Int>): List<Body> = sizes.mapIndexed { i, size ->
        // find a random shape with the given size and create a body from it
        createRandomBodyWithShape(startId + i, shapes.groupBy { it.sideLength }[size]!!.random().id, offsetX, offsetY)
    }

    private fun findNextUnoccupiedScreen(): Point {
        // walk the sequence of next location points until we find one not in occupiedPoints.
        // This will allow clients to be removed from the world, and replaced by new joiners
        val pointGenerator: LocationGenerator = locationGenerator()

        val pointIterator = pointGenerator.generate().iterator()
        while (pointIterator.hasNext()) {
            val point = pointIterator.next()
            if (!occupiedScreens.containsKey(point)) {
                return point
            }
        }
        throw IllegalStateException("Unable to find next unoccupied point.")
    }

    private fun locationGenerator() = when (currentLocationPattern) {
        "grid" -> GridPatternGenerator()
        "right" -> RightGenerator()
        else -> throw Error("Unknown location pattern ${config.locationPattern}")
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
        addEventToAllClients(StatusEvent.CLIENT_CHANGE)
    }

    fun worldBoundary(): Point {
        if (occupiedScreens.isEmpty()) return Point(1, 1)
        return occupiedScreens.keys.bounds().second + Point(1, 1)
    }

    fun broadcastToAllClients(message: String, delaySeconds: Int) {
        /*
           Process:
           - send clients a CLIENT_CMD_EVENT
           - they call back and get an "enable_broadcast" cmd
           - they retrieve the message, and show it

           When we want to turn it off
           - send clients a CLIENT_CMD_EVENT
           - they call back and get a "disable_broadcast" cmd
           - they turn off the message

           For the second phase, ie. turning off, we will need to pause in a background thread for the required "time", and send the second part of the message
         */

        currentBroadcastMessage = message
        addCommandToAllClients(ClientCommand.ENABLE_BROADCAST)

        heartbeatScope.launch {
            disableAllBroadcast(delaySeconds)
        }

    }

    private suspend fun disableAllBroadcast(seconds: Int) {
        delay(1000L * seconds)
        println("sending disabled broadcast to all clients")
        addCommandToAllClients(ClientCommand.DISABLE_BROADCAST)
    }

    fun broadcastToClient(clientId: Int, message: String, delaySeconds: Int) {
        currentBroadcastMessage = message
        addCommandToClient(clientId, ClientCommand.ENABLE_BROADCAST)

        heartbeatScope.launch {
            disableClientBroadcast(clientId, delaySeconds)
        }
    }

    private suspend fun disableClientBroadcast(clientId: Int, seconds: Int) {
        delay(1000L * seconds)
        println("sending disabled broadcast to client $clientId")
        addCommandToClient(clientId, ClientCommand.DISABLE_BROADCAST)
    }

    fun addCommandToAllClients(cmd: ClientCommand) {
        clients.keys.forEach { clientId ->
            addCommandToClient(clientId, cmd)
        }
    }

    private fun removeCommandFromAllClients(cmd: ClientCommand) {
        clients.keys.forEach { id ->
            val clientCmds = clientCommands[id] ?: mutableListOf()
            clientCmds.remove(cmd)
        }
    }

    fun addCommandToClient(clientId: Int, cmd: ClientCommand) {
        val cmds = clientCommands[clientId] ?: mutableListOf()
        cmds.add(cmd)
        clientCommands[clientId] = cmds
        addEvent(clientId, StatusEvent.CLIENT_CMD_EVENT)
    }

    private fun addEventToAllClients(statusEvent: StatusEvent) {
        clients.keys.forEach { id ->
            addEvent(id, statusEvent)
        }
    }

    private fun removeEventFromAllClients(statusEvent: StatusEvent) {
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

    fun getCommands(clientId: Int): ByteArray {
        val commands = clientCommands[clientId] ?: return byteArrayOf()
        clientCommands.remove(clientId)
        return commands.map { it.event.toByte() }.toByteArray()
    }

    fun toggleFrozen() {
        isFrozen = !isFrozen
        if (isFrozen) {
            addEventToAllClients(StatusEvent.FROZEN)
        } else {
            removeEventFromAllClients(StatusEvent.FROZEN)
        }
    }

    // find every VisibleShape for every client
    @Suppress("LocalVariableName")
    private fun findVisibleShapesByClient(): Map<Int, MutableSet<VisibleShape>> {
        val gameClients = clients.values.toList()
        fun clientIdThatOwns(p: Point): Int {
            return gameClients.firstOrNull { c ->
                p.within(c.worldBounds)
            }?.id ?: -1
        }

        if (gameClients.isEmpty()) return emptyMap()

        // initialise the returned map
        val visibleShapesByClient = mutableMapOf<Int, MutableSet<VisibleShape>>()
        gameClients.forEach { client ->
            visibleShapesByClient[client.id] = mutableSetOf()
        }

        currentSimulator.bodies.forEach { body ->
            val bodyWidth = (body.radius * 2).roundToInt()

            val corners = body.bodyCorners(config.scalingFactor, currentSimulator.width, currentSimulator.height)
            // We can use each corner in turn, work out its "centre point" relative to a grid that would cover from that corner, and see if the new centre matches the body centre.
            // Add that to a set of visible points for the shape and "new centre", which will remove duplicates where the corners were in the same non-wrapped position
            // We won't bother optimizing for 1x1 shape, it will just fall out in the wash, 4 calculations on the same point isn't that much
            val cNW = corners[0]
            val cNE = corners[1]
            val cSW = corners[2]
            val cSE = corners[3]

            val nd2_1 = (bodyWidth / 2 - 1) * config.scalingFactor
            val nd2 = (bodyWidth / 2) * config.scalingFactor
            val n_1d2 = ((bodyWidth - 1) / 2) * config.scalingFactor

            val centre1 = cNW + if (bodyWidth % 2 == 0) Point(nd2_1, nd2_1) else Point(n_1d2, n_1d2)
            val centre2 = cNE + if (bodyWidth % 2 == 0) Point(-nd2, nd2_1) else Point(-n_1d2, n_1d2)
            val centre3 = cSW + if (bodyWidth % 2 == 0) Point(nd2_1, -nd2) else Point(n_1d2, -n_1d2)
            val centre4 = cSE + if (bodyWidth % 2 == 0) Point(-nd2, -nd2) else Point(-n_1d2, -n_1d2)

            // add the visible shape to the client's list. dupes will be removed, and this also caters for both wrapping and no wrapping
            visibleShapesByClient[clientIdThatOwns(cNW)]?.add(VisibleShape(body.shapeId, centre1, body.id))
            visibleShapesByClient[clientIdThatOwns(cNE)]?.add(VisibleShape(body.shapeId, centre2, body.id))
            visibleShapesByClient[clientIdThatOwns(cSW)]?.add(VisibleShape(body.shapeId, centre3, body.id))
            visibleShapesByClient[clientIdThatOwns(cSE)]?.add(VisibleShape(body.shapeId, centre4, body.id))

        }
        return visibleShapesByClient
    }

    fun addBody(size: Int) {
        val bodies = createBodies(currentSimulator.bodies.count(), 0, 0, listOf(size))
        currentSimulator.addBodies(bodies)
        addEventToAllClients(StatusEvent.OBJECT_CHANGE)
    }

    fun resetWorld() {
        currentSimulator.reset()
        addEventToAllClients(StatusEvent.OBJECT_CHANGE)
    }

    fun increaseSpeed() {
        currentSimulator.bodies.forEach { body -> body.velocity.mul(1.05f) }
    }

    fun decreaseSpeed() {
        currentSimulator.bodies.forEach { body -> body.velocity.div(1.05f) }
    }

    companion object {
        // The world size will be its boundary * these values.
        const val SCREEN_WIDTH = 160
        const val SCREEN_HEIGHT = 88
    }
}
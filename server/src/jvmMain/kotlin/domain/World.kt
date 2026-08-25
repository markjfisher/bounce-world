package domain

import bw.BodyShared
import bw.GameClientShared
import bw.WorldShared
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
import logger
import org.joml.Vector2f
import simulator.WorldSimulator
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.time.TimeSource

interface WorldUpdateListener {
    suspend fun update(state: WorldShared)
}

open class World(
    private val config: WorldConfig,
    private val wrappedSimulator: WorldSimulator,
    private val boundedSimulator: WorldSimulator
) {
    private val simulationScope = CoroutineScope(Dispatchers.Default)
    private val heartbeatScope = CoroutineScope(Dispatchers.IO)

    private val updateListeners: MutableSet<WorldUpdateListener> = mutableSetOf()
    private val startTime: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow() // LocalDateTime.now().minusMonths(1).minusDays(5).minusHours(2).minusMinutes(34)
    var currentUptime: String = formatUptime(startTime, TimeSource.Monotonic.markNow())

    fun addListener(listener: WorldUpdateListener) {
        updateListeners.add(listener)
    }

    private fun notifyListeners() {
        updateListeners.forEach { listener ->
            val worldShared = toWorldShared()
            heartbeatScope.launch {
                listener.update(worldShared)
            }
        }
    }

    // the data about clients
    private val clients = mutableMapOf<Int, GameClient>()
    fun clients(): List<GameClient> = clients.values.toList()
    fun clientIds(): Set<Int> = clients.keys.toSet()
    fun setClients(newClients: Map<Int, GameClient>) {
        clients.clear()
        clients.putAll(newClients)
    }

    fun getWorldWidth() = clients.values.maxOfOrNull { it.region.x + it.region.width } ?: config.width
    fun getWorldHeight() = clients.values.maxOfOrNull { it.region.y + it.region.height } ?: config.height

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
    private val stepTime = 1f / config.updatesPerSecond
    var isWrapping = config.enableWrapping

    val currentSimulator: WorldSimulator
        get() = if (isWrapping) wrappedSimulator else boundedSimulator

    val currentClientVisibleShapes = mutableMapOf<Int, MutableSet<VisibleShape>>()

    fun updateHeartbeat(id: Int) {
        clientHeartbeats[id] = System.currentTimeMillis()
    }

    init {
        if (!isStarted && config.shouldAutoStart) {
            simulationScope.launch {
                runSimulation()
            }
            heartbeatScope.launch {
                checkClientsStillConnected()
            }
        }
    }

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
        stopped = false

        addEventToAllClients(StatusEvent.OBJECT_CHANGE)
    }

    private suspend fun checkClientsStillConnected() {
        while (!stopped) {
            // creating a list to iterate over instead of directly on the keys stops the concurrent update error as we're not modifying this list
            val clientIds = clients.keys.toList() // use the clients directly rather than channels, as they may not have sent any data yet, so aren't in the channels list, but we do have them in the initial heartbeats
            clientIds.forEach { clientId ->
                val sinceHeartbeat = System.currentTimeMillis() - (clientHeartbeats[clientId] ?: 0)
                if (sinceHeartbeat > config.heartbeatTimeoutMillis) {
                    logger.info("No heartbeat from client ${clients[clientId]?.name ?: "UNKNOWN"} for $sinceHeartbeat ms, unregistering client.")
                    unregisterClient(clientId)
                }
            }
            delay(5000L)
        }
    }

    private suspend fun runSimulation() {
        var started: Long
        isStarted = true

        currentUptime = formatUptime(startTime, TimeSource.Monotonic.markNow())

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
                    if (bodyIdsForCurrentClient.intersect(currentSimulator.collisionsCopy()).isNotEmpty()) {
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

            val newUptime = formatUptime(startTime, TimeSource.Monotonic.markNow())
            if (newUptime != currentUptime) {
                currentUptime = newUptime
                notifyListeners()
            }
        }
    }

    private fun unregisterClient(clientId: Int) {
        removeClient(clientId)
    }

    open fun addClient(gameClient: GameClient) {
        val nextPoint = findNextUnoccupiedScreen()
        gameClient.position = nextPoint
        if (gameClient.worldSize.width <= 0 || gameClient.worldSize.height <= 0) {
            gameClient.worldSize = ScreenSize(config.width, config.height)
        }
        clients[gameClient.id] = gameClient
        occupiedScreens[nextPoint] = gameClient

        recalculateLayout()
    }

    fun createClient(gameClientInfo: GameClientInfo): GameClient {
        val client = GameClient(
            id = nextFreeClientId(),
            name = gameClientInfo.name,
            version = gameClientInfo.version,
            screenSize = gameClientInfo.screenSize,
            worldSize = gameClientInfo.worldSize ?: ScreenSize(config.width, config.height),
            capabilities = gameClientInfo.capabilities,
        )
        addClient(client)
        clientHeartbeats[client.id] = System.currentTimeMillis()
        addEventToAllClients(StatusEvent.CLIENT_CHANGE)
        return client
    }

    // Client ids travel on the wire as a single unsigned byte (0 is reserved for errors),
    // so allocate the lowest free id in 1..255. This recycles ids of disconnected clients
    // and wraps past 255, skipping any id still in use by a connected client.
    private fun nextFreeClientId(): Int {
        for (id in 1..255) {
            if (clients[id] == null) return id
        }
        throw IllegalStateException("No free client id available (255 clients already connected)")
    }

    fun at(point: Point): GameClient? = occupiedScreens[point]
    fun getClient(id: Int): GameClient? = clients[id]

    // grid is the screen location to create this body in, e.g. (0,0) for first client, (1,0) for second, etc.
    fun createBody(shapeId: Int, grid: Point): Body {
        val shape = shapes.first { it.id == shapeId }
        val nextId = currentSimulator.nextBodyId()
        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
        var velocity = Vector2f(cos(angle), sin(angle)).mul(config.initialSpeed)
        // ensure the client didn't ask for a location outside the world boundary, if they did, put it in the first screen (0,0)
        val correctedGrid = when {
            grid.x >= worldBoundary().x || grid.y >= worldBoundary().y || grid.x < 0 || grid.y < 0 -> Point(0, 0)
            else -> grid
        }
        val region = occupiedScreens[correctedGrid]?.region ?: ClientRegion(
            correctedGrid.x * config.width,
            correctedGrid.y * config.height,
            config.width,
            config.height
        )
        // Create a position that's within a screen's boundaries, but will be inside the particular client's boundary that created it.
        // caters for the radius of the shape by reducing the possible x/y coordinates it starts at to be within a screen's size
        val pos = Vector2f(
            region.x + Random.nextFloat() * (region.width.toFloat() - shape.sideLength - 5f) + shape.sideLength / 2f + 2,
            region.y + Random.nextFloat() * (region.height.toFloat() - shape.sideLength - 5f) + shape.sideLength / 2f + 2
        )
        val body = Body.from(id = nextId, position = pos, velocity = velocity, shape = shape)
        if (isWrapping) {
            applyAntiDrift(body)
        }
        return body
    }

    companion object {
        // Anti-drift spawn tuning. In a wrapping world total momentum P and (approximately)
        // total spin L = sum(I*w) are conserved by collisions, so random spawning accumulates a
        // net drift every body eventually shares. New bodies oppose the current totals:
        //   - if the required opposing speed/spin is below the respective MIN threshold, the
        //     world is already near equilibrium, so we spawn fully randomly instead — which
        //     naturally re-injects drift and keeps the demo lively rather than freezing it.
        //   - otherwise the new body points against the drift with a random fraction
        //     [MIN_FRACTION, 1] of the exactly-cancelling magnitude ("chipping away").
        private const val ANTI_DRIFT_MIN_SPEED = 0.25f
        private const val ANTI_DRIFT_MIN_SPIN = 0.1f
        private const val ANTI_DRIFT_MIN_FRACTION = 0.5f
        private const val SPAWN_MAX_SPIN = 1.0f
    }

    /**
     * Bias a freshly created body's linear velocity and spin to oppose the current net momentum
     * and spin of the wrapping world. See the companion-object notes for the strategy.
     */
    private fun applyAntiDrift(body: Body) {
        currentSimulator.withBodiesRead { existing ->
            var px = 0f
            var py = 0f
            var totalAngularMomentum = 0f
            for (other in existing) {
                px += other.mass * other.velocity.x
                py += other.mass * other.velocity.y
                totalAngularMomentum += spinInertia(other) * other.angularVelocity
            }

            // linear: exact cancellation would be v = -P / m_new; take a random fraction of it
            val neededVx = -px / body.mass
            val neededVy = -py / body.mass
            val neededSpeed = sqrt(neededVx * neededVx + neededVy * neededVy)
            if (neededSpeed >= ANTI_DRIFT_MIN_SPEED) {
                val fraction = ANTI_DRIFT_MIN_FRACTION + Random.nextFloat() * (1f - ANTI_DRIFT_MIN_FRACTION)
                val speed = neededSpeed * fraction
                body.velocity.set(neededVx / neededSpeed * speed, neededVy / neededSpeed * speed)
            } else {
                // near equilibrium: full random direction and speed, which re-injects fresh drift
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                body.velocity.set(cos(angle), sin(angle)).mul(config.initialSpeed)
            }

            // spin: exact cancellation would be w = -L / I_new
            val inertia = spinInertia(body)
            val neededSpin = -totalAngularMomentum / inertia
            body.angularVelocity =
                if (abs(neededSpin) >= ANTI_DRIFT_MIN_SPIN) {
                    val fraction = ANTI_DRIFT_MIN_FRACTION + Random.nextFloat() * (1f - ANTI_DRIFT_MIN_FRACTION)
                    neededSpin * fraction
                } else {
                    (Random.nextFloat() * 2f - 1f) * SPAWN_MAX_SPIN
                }
        }
    }

    private fun spinInertia(body: Body): Float = 0.5f * body.mass * body.radius * body.radius

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
        logger.info("Removing client $client")
        clients.remove(client.id)
        clientHeartbeats.remove(client.id)
        statusEvents.remove(client.id)
        val entriesForClient = occupiedScreens.filterValues { c -> c.id == id }
        if (entriesForClient.isNotEmpty()) {
            occupiedScreens.remove(entriesForClient.keys.first())
        }
        recalculateLayout()
        addEventToAllClients(StatusEvent.CLIENT_CHANGE)
    }

    private fun recalculateLayout() {
        val oldWidth = currentSimulator.width
        val oldHeight = currentSimulator.height

        when (currentLocationPattern) {
            "grid", "right" -> recalculateTrackLayout()
            else -> throw Error("Unknown location pattern ${config.locationPattern}")
        }

        val newWidth = getWorldWidth()
        val newHeight = getWorldHeight()
        resizeSimulators(newWidth, newHeight)

        if (newWidth < oldWidth || newHeight < oldHeight) {
            reconcileBodiesAfterResize(newWidth, newHeight)
        }
    }

    private fun recalculateTrackLayout() {
        if (clients.isEmpty()) return

        val columnWidths = occupiedScreens.entries
            .groupBy({ it.key.x }, { it.value.worldSize.width })
            .mapValues { (_, widths) -> widths.max() }
        val rowHeights = occupiedScreens.entries
            .groupBy({ it.key.y }, { it.value.worldSize.height })
            .mapValues { (_, heights) -> heights.max() }

        val sortedColumns = columnWidths.keys.sorted()
        val sortedRows = rowHeights.keys.sorted()

        val columnOffsets = mutableMapOf<Int, Int>()
        var x = 0
        sortedColumns.forEach { column ->
            columnOffsets[column] = x
            x += columnWidths.getValue(column)
        }

        val rowOffsets = mutableMapOf<Int, Int>()
        var y = 0
        sortedRows.forEach { row ->
            rowOffsets[row] = y
            y += rowHeights.getValue(row)
        }

        occupiedScreens.forEach { (slot, client) ->
            client.setRegion(
                x = columnOffsets.getValue(slot.x),
                y = rowOffsets.getValue(slot.y),
                width = client.worldSize.width,
                height = client.worldSize.height
            )
        }
    }

    private fun resizeSimulators(width: Int, height: Int) {
        wrappedSimulator.width = width
        wrappedSimulator.height = height
        boundedSimulator.width = width
        boundedSimulator.height = height
    }

    private fun reconcileBodiesAfterResize(width: Int, height: Int) {
        currentSimulator.updateBodies { bodies ->
            bodies.forEach { body ->
                if (isWrapping) {
                    body.position.x = wrap(body.position.x, width)
                    body.position.y = wrap(body.position.y, height)
                    body.intendedPosition.set(body.position)
                } else {
                    val minX = body.radius
                    val maxX = max(minX, width - body.radius)
                    val minY = body.radius
                    val maxY = max(minY, height - body.radius)
                    body.position.x = body.position.x.coerceIn(minX, maxX)
                    body.position.y = body.position.y.coerceIn(minY, maxY)
                    body.intendedPosition.set(body.position)
                }
            }
        }
    }

    private fun wrap(value: Float, max: Int): Float {
        return if (value < 0) {
            (value % max + max) % max
        } else {
            value % max
        }
    }

    fun worldBoundary(): Point {
        if (occupiedScreens.isEmpty()) return Point(1, 1)
        return occupiedScreens.keys.bounds().second + Point(1, 1)
    }

    fun broadcastToAllClients(message: String, delaySeconds: Int) {/*
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

        // max message size is 119 chars (fits in 120 with padded 0 char for C strings on client)
        currentBroadcastMessage = message.substring(0, min(119, message.length))
        addCommandToAllClients(ClientCommand.ENABLE_BROADCAST)

        heartbeatScope.launch {
            disableAllBroadcast(delaySeconds)
        }

    }

    private suspend fun disableAllBroadcast(seconds: Int) {
        delay(1000L * seconds)
        logger.info("sending disabled broadcast to all clients")
        addCommandToAllClients(ClientCommand.DISABLE_BROADCAST)
    }

    fun broadcastToClient(clientId: Int, message: String, delaySeconds: Int) {
        currentBroadcastMessage = message.substring(0, min(119, message.length))
        addCommandToClient(clientId, ClientCommand.ENABLE_BROADCAST)

        heartbeatScope.launch {
            disableClientBroadcast(clientId, delaySeconds)
        }
    }

    private suspend fun disableClientBroadcast(clientId: Int, seconds: Int) {
        delay(1000L * seconds)
        logger.info("sending disabled broadcast to client $clientId")
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
        notifyListeners()
    }

    private fun removeEventFromAllClients(statusEvent: StatusEvent) {
        clients.keys.forEach { id ->
            val clientEvents = statusEvents[id] ?: mutableSetOf()
            clientEvents.remove(statusEvent)
        }
        notifyListeners()
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
        val commands = clientCommands[clientId] ?: return byteArrayOf(0)
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

    // Find every VisibleShape for every client, preserving sub-world-unit float precision so
    // hi-res clients can scale smoothly to pixels.
    //
    // For each body we consider its position plus, in a wrapping world, copies offset by a full
    // world width/height across the seams. A copy is visible to a client when the body's bounding
    // box overlaps that client's region — this gives clients at region edges the partially
    // visible (slightly out-of-region) coordinates they need to draw a body straddling the edge,
    // including bodies straddling the wrap seam itself. Duplicate copies collapse via the set.
    internal fun findVisibleShapesByClient(): Map<Int, MutableSet<VisibleShape>> {
        val gameClients = clients.values.toList()
        if (gameClients.isEmpty()) return emptyMap()

        val visibleShapesByClient = mutableMapOf<Int, MutableSet<VisibleShape>>()
        gameClients.forEach { client ->
            visibleShapesByClient[client.id] = mutableSetOf()
        }

        val worldWidth = currentSimulator.width.toFloat()
        val worldHeight = currentSimulator.height.toFloat()

        currentSimulator.forEachBody { body ->
            val xs = mutableListOf(body.position.x)
            val ys = mutableListOf(body.position.y)
            if (isWrapping) {
                xs += body.position.x - worldWidth
                xs += body.position.x + worldWidth
                ys += body.position.y - worldHeight
                ys += body.position.y + worldHeight
            }

            for (client in gameClients) {
                val rx = client.region.x.toFloat()
                val ry = client.region.y.toFloat()
                val rw = client.region.width.toFloat()
                val rh = client.region.height.toFloat()
                for (x in xs) {
                    if (x + body.radius < rx || x - body.radius > rx + rw) continue
                    for (y in ys) {
                        if (y + body.radius < ry || y - body.radius > ry + rh) continue
                        visibleShapesByClient[client.id]?.add(
                            VisibleShape(body.shapeId, Vector2f(x, y), body.id, body.angle, body.angularVelocity)
                        )
                    }
                }
            }
        }
        return visibleShapesByClient
    }

    // original implementation just putting a new shape of this size in the first screen
    fun addRandomBodyWithSize(size: Int) {
        val randomShapeId = shapes.groupBy { it.sideLength }[size]!!.random().id
        val body = createBody(randomShapeId, Point(0, 0))
        currentSimulator.addBodies(listOf(body))
        addEventToAllClients(StatusEvent.OBJECT_CHANGE)
    }

    // add specific shape to world in location grid. Allows clients to add to themselves (or others!)
    fun addBody(shapeId: Int, grid: Point) {
        val body = createBody(shapeId, grid)
        currentSimulator.addBodies(listOf(body))
        addEventToAllClients(StatusEvent.OBJECT_CHANGE)
    }

    fun resetWorld() {
        currentSimulator.reset()
        addEventToAllClients(StatusEvent.OBJECT_CHANGE)
    }

    fun increaseSpeed() {
        currentSimulator.forEachBody { body -> body.velocity.mul(1.05f) }
    }

    fun decreaseSpeed() {
        currentSimulator.forEachBody { body -> body.velocity.div(1.05f) }
    }
}

fun World.toWorldShared() = WorldShared(
    width = getWorldWidth(),
    height = getWorldHeight(),
    upTime = currentUptime,
    clients = clients().associate { client ->
        client.id to GameClientShared(
            id = client.id,
            name = client.name,
            version = client.version,
            position = Pair(client.position.x, client.position.y),
            screenSize = Pair(client.screenSize.width, client.screenSize.height)
        )
    },
    isFrozen = isFrozen,
    isWrapping = isWrapping,
    bodies = currentSimulator.mapBodies { body ->
        BodyShared(
            id = body.id,
            position = Pair(body.position.x, body.position.y),
            velocity = Pair(body.velocity.x, body.velocity.y),
            mass = body.mass,
            radius = body.radius,
            shapeId = body.shapeId
        )
    }
)

fun formatUptime(startTime: TimeSource.Monotonic.ValueTimeMark, endTime: TimeSource.Monotonic.ValueTimeMark): String {
    val elapsed = endTime - startTime
    return elapsed.toComponents { h, m, _, _ ->
        buildString {
            val d = elapsed.inWholeDays
            if (d > 0) {
                append("$d day")
                if (d != 1L) append("s")
                append(", ")
            }
            if (h > 0 || d > 0) {
                val partHours = h - d * 24
                append("$partHours hour")
                if (partHours != 1L) append("s")
                append(", ")
            }
            append("$m min")
            if (m != 1) append("s")
        }
    }
}

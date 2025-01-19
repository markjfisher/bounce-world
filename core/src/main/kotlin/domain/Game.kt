package domain

import data.Quadtree
import items.GameItem
import items.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import logger
import org.joml.Vector2f
import simulator.GameSimulator
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.time.TimeSource


open class Game (
    // private val gameConfig: GameConfig,
    private val simulator: GameSimulator
) {
    val simulationScope = CoroutineScope(Dispatchers.Default)
    val heartbeatScope = CoroutineScope(Dispatchers.IO)

    private val clientMap = mutableMapOf<Int, GameClientNew>()
    protected val clientHeartbeats = mutableMapOf<Int, Long>()
    private val updateListeners: MutableSet<GameUpdateListener> = mutableSetOf()
    private val startTime: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()
    private var currentUptime: String = formatUptime(startTime, TimeSource.Monotonic.markNow())

    // part of simulator?
    private val gameEvents = mutableMapOf<Int, MutableSet<GameEvent>>()

    // This maps the player gameItem to a client id, so we can find their screen sizes etc
    private val playerIdToClientMap = mutableMapOf<Int, Int>()

    // should shapes belong to the simulator? Maybe not, they are game things, simulator moves items.
    // An item can be a body with shape, or a player etc.
    // we have List<GameItem> in the simulator

    fun clients(): List<GameClientNew> = clientMap.values.toList()
    fun clientIds(): Set<Int> = clientMap.keys.toSet()
    fun getClient(id: Int): GameClientNew? = clientMap[id]
    fun gameWidth() = simulator.width
    fun gameHeight() = simulator.height

    open fun addClient(gameClient: GameClientNew, position: Vector2f): Player {
        clientMap[gameClient.id] = gameClient
        val player = Player(
            id = simulator.items.size + 1,
            position = position,
            velocity = Vector2f(0f, 0f),
            direction = Math.PI / 2.0,
            mass = 1.0f,
            radius = 1.0f
        )
        simulator.addItem(player)
        playerIdToClientMap[player.id] = gameClient.id
        return player
    }

    open fun removeClient(clientId: Int) {
        val client = getClient(clientId) ?: return
        logger.info("Removing client $client")
        clientMap.remove(client.id)
        clientHeartbeats.remove(client.id)
        gameEvents.remove(client.id)
    }

    private fun gameToViewCoordinates(player: Player, point: Vector2f): Vector2f {
        // Translate the point to be relative to the player's position
        val translatedX = point.x - player.position.x
        val translatedY = point.y - player.position.y

        // Correctly rotate the translated point to align the player's forward direction with the "up" direction in the view
        // Adjusting the rotation formula to correctly handle the player's direction
        val direction = -(player.direction - Math.PI / 2)
        val rotatedX = translatedX * cos(direction) - translatedY * sin(direction)
        val rotatedY = translatedX * sin(direction) + translatedY * cos(direction)

        return Vector2f(rotatedX.toFloat(), rotatedY.toFloat())
    }

//    fun determineVisibleItems(): Map<Int, Set<VisibleItem>> {
//        val visibleItemsMap = mutableMapOf<Int, Set<VisibleItem>>()
//
//        // TODO: turn into fold? useful like this until we test and debug it
//        // TODO: is this the correct way around? Look for player objects, or should we start with the known clients? Does it matter?
//        simulator.items.filterIsInstance<Player>().forEach loop@{ player ->
//            val clientIdForPlayer = playerIdToClientMap[player.id] ?: return@loop
//            val client = clientMap[clientIdForPlayer] ?: return@loop
//            visibleItemsMap[player.id] = simulator.items.filter { item -> item.id != player.id && isItemVisible(player, client, item, gameWidth(), gameHeight()) }
//                .map { VisibleItem(it.id, it.position) }
//                .toSet()
//        }
//
//        return visibleItemsMap
//    }

    fun determineVisibleItems(): Map<Int, Set<VisibleItem>> {
        val visibleItemsMap = mutableMapOf<Int, Set<VisibleItem>>()

        val quadtree = Quadtree(gameWidth(), gameHeight(), 1, 6)
        simulator.items.forEach { item ->
            // Create a rectangle for the location of the current item
            val bound = Vector2f(item.radius, item.radius).mul(1.05f) // Assuming each item has a radius property
            val upperLeft = Vector2f(item.position).sub(bound)
            val lowerRight = Vector2f(item.position).add(bound)
            quadtree.insert(item.id, upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y)
        }

        simulator.items.filterIsInstance<Player>().forEach loop@{ player ->
            val clientIdForPlayer = playerIdToClientMap[player.id] ?: return@loop
            val client = clientMap[clientIdForPlayer] ?: return@loop

            // Determine the range to query based on the client's screen size
            val queryRange = max(client.screenWidth, client.screenHeight).toFloat()
            val queryBound = Vector2f(queryRange, queryRange)
            val upperLeftQuery = Vector2f(player.position).sub(queryBound)
            val lowerRightQuery = Vector2f(player.position).add(queryBound)

            // Query the QuadTree for items near the player
            val nearbyItemIds = quadtree.queryWithIds(upperLeftQuery.x, upperLeftQuery.y, lowerRightQuery.x, lowerRightQuery.y)
                .map { it.second }
                .filterNot { it == player.id }

            // Filter and transform the nearby items to VisibleItems if they are visible
            visibleItemsMap[player.id] = nearbyItemIds.mapNotNull { itemId ->
                simulator.items.find { it.id == itemId }
            }.filter { item ->
                isItemVisible(player, client, item, gameWidth(), gameHeight())
            }.map { VisibleItem(it.id, it.position) }
                .toSet()
        }

        return visibleItemsMap
    }

    private fun isItemVisible(player: Player, client: GameClientNew, item: GameItem, width: Int, height: Int): Boolean {
        val corners = item.itemCorners(width, height).map { Vector2f(it.x.toFloat(), it.y.toFloat()) }
        return corners.any { corner ->
            val viewCoords = gameToViewCoordinates(player, corner)
            // Check if the corner is within the screen bounds
            viewCoords.x in -client.screenWidth / 2f..client.screenWidth / 2f &&
                    viewCoords.y in -client.screenHeight / 2f..client.screenHeight / 2f
        }
    }


    fun addListener(listener: GameUpdateListener) {
        updateListeners.add(listener)
    }

    private fun notifyListeners() {
        updateListeners.forEach { listener ->
            heartbeatScope.launch {
                listener.update(this@Game)
            }
        }
    }
}

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
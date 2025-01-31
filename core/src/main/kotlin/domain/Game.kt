package domain

import data.QuadTree
import geometry.translatePointToTargetViewCoordinates
import items.GameItem
import items.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logger
import org.joml.Vector2f
import simulator.GameSimulator
import string.formatUptime
import wrapped.findClosestWrappedPosition
import kotlin.time.TimeSource


open class Game(
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
    protected var running = true

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

    // creates the map of player.id to set of item ids that can be seen, and their position RELATIVE to the player's direction
    // The player itself is excluded from the items
    fun determineVisibleItems(isWrapped: Boolean): Map<Int, Set<VisibleItem>> {
        val visibleItemsMap = mutableMapOf<Int, Set<VisibleItem>>()

        val quadtree = QuadTree(gameWidth(), gameHeight(), 1, 6)
        simulator.items.forEach { item ->
            // Create a slightly expanded rectangle for the location of the current item
            val bound = Vector2f(item.radius, item.radius).mul(1.05f)
            val lowerLeft = Vector2f(item.position).sub(bound)
            val upperRight = Vector2f(item.position).add(bound)
            // println("adding item with boundary: $lowerLeft to $upperRight")
            quadtree.insert(item.id, lowerLeft.x, lowerLeft.y, upperRight.x, upperRight.y)
        }

        // for every player, find the closest items
        simulator.items.filterIsInstance<Player>().forEach loop@{ player ->
            val clientIdForPlayer = playerIdToClientMap[player.id] ?: return@loop
            val client = clientMap[clientIdForPlayer] ?: return@loop

            val halfScreenWidth = client.screenWidth.toFloat() / 2f
            val halfScreenHeight = client.screenHeight.toFloat() / 2f

            // Determine the range to query based on the client's screen size
            val queryBound = Vector2f(halfScreenWidth, halfScreenHeight)
            val lowerLeftQuery = Vector2f(player.position).sub(queryBound)
            val upperRightQuery = Vector2f(player.position).add(queryBound)

            // Query the QuadTree for items near the player that aren't the player themselves
            val nearbyItemIds = quadtree.queryWithIds(lowerLeftQuery.x, lowerLeftQuery.y, upperRightQuery.x, upperRightQuery.y)
                .map { it.second }
                .filterNot { it == player.id }
                .toMutableList()

            if (isWrapped) {
                val isCloseToLeft = player.position.x - halfScreenWidth < 0f
                val isCloseToRight = player.position.x + halfScreenWidth > gameWidth()
                val isCloseToTop = player.position.y - halfScreenHeight < 0f
                val isCloseToBottom = player.position.y + halfScreenHeight > gameHeight()

                // additionally find edges within the player's view and wrap them to the other side
                // of it and query again to find wrapped items they can see.
                if (isCloseToLeft) {
                    nearbyItemIds += quadtree.queryWithIds(lowerLeftQuery.x + gameWidth(), lowerLeftQuery.y, upperRightQuery.x + gameWidth(), upperRightQuery.y)
                        .map { it.second }
                        .filterNot { it == player.id }
                }
                if (isCloseToRight) {
                    nearbyItemIds += quadtree.queryWithIds(lowerLeftQuery.x - gameWidth(), lowerLeftQuery.y, upperRightQuery.x - gameWidth(), upperRightQuery.y)
                        .map { it.second }
                        .filterNot { it == player.id }
                }
                if (isCloseToTop) {
                    nearbyItemIds += quadtree.queryWithIds(lowerLeftQuery.x, lowerLeftQuery.y + gameHeight(), upperRightQuery.x - gameWidth(), upperRightQuery.y + gameHeight())
                        .map { it.second }
                        .filterNot { it == player.id }
                }
                if (isCloseToBottom) {
                    nearbyItemIds += quadtree.queryWithIds(lowerLeftQuery.x, lowerLeftQuery.y - gameHeight(), upperRightQuery.x - gameWidth(), upperRightQuery.y - gameHeight())
                        .map { it.second }
                        .filterNot { it == player.id }
                }
                if (isCloseToLeft && isCloseToTop) {
                    nearbyItemIds += quadtree.queryWithIds(lowerLeftQuery.x + gameWidth(), lowerLeftQuery.y + gameHeight(), upperRightQuery.x + gameWidth(), upperRightQuery.y + gameHeight())
                        .map { it.second }
                        .filterNot { it == player.id }
                }
                if (isCloseToLeft && isCloseToBottom) {
                    nearbyItemIds += quadtree.queryWithIds(lowerLeftQuery.x + gameWidth(), lowerLeftQuery.y - gameHeight(), upperRightQuery.x + gameWidth(), upperRightQuery.y - gameHeight())
                        .map { it.second }
                        .filterNot { it == player.id }
                }
                if (isCloseToRight && isCloseToTop) {
                    nearbyItemIds += quadtree.queryWithIds(lowerLeftQuery.x - gameWidth(), lowerLeftQuery.y + gameHeight(), upperRightQuery.x - gameWidth(), upperRightQuery.y + gameHeight())
                        .map { it.second }
                        .filterNot { it == player.id }
                }
                if (isCloseToRight && isCloseToBottom) {
                    nearbyItemIds += quadtree.queryWithIds(lowerLeftQuery.x - gameWidth(), lowerLeftQuery.y - gameHeight(), upperRightQuery.x - gameWidth(), upperRightQuery.y - gameHeight())
                        .map { it.second }
                        .filterNot { it == player.id }
                }
            }

            // Filter and transform the nearby items to VisibleItems if they are visible
            visibleItemsMap[player.id] = nearbyItemIds
                .mapNotNull { itemId -> simulator.items.find { it.id == itemId } }
                .filter { item ->
                    // we need this because we have to consider the player's orientation and qt boxes are rounded so may overlap even when not really overlapping
                    isItemVisible(player, client, item, gameWidth(), gameHeight(), isWrapped)
                }.map { item ->
                    // convert the item to the player's view considering the player as pointing north.
                    val translatedItemLocation = translatePointToTargetViewCoordinates(player.position, player.direction, item.position)
                    VisibleItem(item.id, translatedItemLocation)
                }
                .toSet()
        }

        return visibleItemsMap
    }

    private fun isItemVisible(player: Player, client: GameClientNew, item: GameItem, width: Int, height: Int, isWrapped: Boolean): Boolean {
        // the player that caused a close match may be a wrapped version, but we only have the original here
        // so work out the closest wrapped player location to the item

        val playerPosition = if (isWrapped) findClosestWrappedPosition(item.position, player.position, width, height) else player.position

        val corners = item.itemCorners(width, height, false).map { Vector2f(it.x.toFloat(), it.y.toFloat()) }
        return corners.any { corner ->
            val viewCoords = translatePointToTargetViewCoordinates(playerPosition, player.direction, corner)
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

    suspend fun checkClientsStillConnected(timeoutMillis: Long) {
        while (running) {
            val clientIds = clientMap.keys.toList() // use the clients directly rather than channels, as they may not have sent any data yet, so aren't in the channels list, but we do have them in the initial heartbeats
            clientIds.forEach { clientId ->
                val sinceHeartbeat = System.currentTimeMillis() - (clientHeartbeats[clientId] ?: 0)
                if (sinceHeartbeat > timeoutMillis) {
                    logger.info("No heartbeat from client ${clientMap[clientId]?.name ?: "UNKNOWN"} for $sinceHeartbeat ms, unregistering client.")
                    removeClient(clientId)
                }
            }
            delay(5000L)
        }
    }

}

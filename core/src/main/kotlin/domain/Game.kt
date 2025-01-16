package domain

import geometry.Point
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import logger
import simulator.GameSimulator
import kotlin.math.roundToInt
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

    // should shapes belong to the simulator? Maybe not, they are game things, simulator moves items.
    // An item can be a body with shape, or a player etc.
    // we have List<GameItem> in the simulator

    fun clients(): List<GameClientNew> = clientMap.values.toList()
    fun clientIds(): Set<Int> = clientMap.keys.toSet()
    fun getClient(id: Int): GameClientNew? = clientMap[id]
    fun getGameWidth() = simulator.width
    fun getGameHeight() = simulator.height

    open fun addClient(gameClient: GameClientNew) {
        clientMap[gameClient.id] = gameClient
    }

    open fun removeClient(clientId: Int) {
        val client = getClient(clientId) ?: return
        logger.info("Removing client $client")
        clientMap.remove(client.id)
        clientHeartbeats.remove(client.id)
        gameEvents.remove(client.id)
    }

    protected fun findVisibleItemsByClient(): Map<Int, MutableSet<VisibleShape>> {
        val gameClients = clients()
        fun clientIdThatOwns(p: Point): Int {
            return gameClients.firstOrNull { c ->
                p.within(c.worldBounds)
            }?.id ?: -1
        }

        if (gameClients.isEmpty()) return emptyMap()

        // initialise the returned map
        val visibleItemsByClient = mutableMapOf<Int, MutableSet<VisibleShape>>()
        gameClients.forEach { client ->
            visibleItemsByClient[client.id] = mutableSetOf()
        }

        simulator.items.forEach { item ->
            val bodyWidth = (item.radius * 2).roundToInt()

            val corners = item.bodyCorners(simulator.width, simulator.height)
            // We can use each corner in turn, work out its "centre point" relative to a grid that would cover from that corner, and see if the new centre matches the body centre.
            // Add that to a set of visible points for the shape and "new centre", which will remove duplicates where the corners were in the same non-wrapped position
            // We won't bother optimizing for 1x1 shape, it will just fall out in the wash, 4 calculations on the same point isn't that much
            val cNW = corners[0]
            val cNE = corners[1]
            val cSW = corners[2]
            val cSE = corners[3]

            val nd2_1 = bodyWidth / 2 - 1
            val nd2 = bodyWidth / 2
            val n_1d2 = (bodyWidth - 1) / 2

            val centre1 = cNW + if (bodyWidth % 2 == 0) Point(nd2_1, nd2_1) else Point(n_1d2, n_1d2)
            val centre2 = cNE + if (bodyWidth % 2 == 0) Point(-nd2, nd2_1) else Point(-n_1d2, n_1d2)
            val centre3 = cSW + if (bodyWidth % 2 == 0) Point(nd2_1, -nd2) else Point(n_1d2, -n_1d2)
            val centre4 = cSE + if (bodyWidth % 2 == 0) Point(-nd2, -nd2) else Point(-n_1d2, -n_1d2)

            // add the visible shape to the client's list. dupes will be removed, and this also caters for both wrapping and no wrapping
            visibleItemsByClient[clientIdThatOwns(cNW)]?.add(VisibleShape(item.shapeId, centre1, item.id))
            visibleItemsByClient[clientIdThatOwns(cNE)]?.add(VisibleShape(item.shapeId, centre2, item.id))
            visibleItemsByClient[clientIdThatOwns(cSW)]?.add(VisibleShape(item.shapeId, centre3, item.id))
            visibleItemsByClient[clientIdThatOwns(cSE)]?.add(VisibleShape(item.shapeId, centre4, item.id))

        }
        return visibleItemsByClient
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
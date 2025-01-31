package game

import config.BWGameConfig
import domain.BWGameClient
import domain.Game
import domain.VisibleShape
import domain.formatUptime
import geometry.GridPatternGenerator
import geometry.LocationGenerator
import geometry.Point
import geometry.RightGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logger
import simulator.GameSimulator
import string.formatUptime
import kotlin.time.TimeSource

class BWGame(
    private val config: BWGameConfig,
    private val simulator: GameSimulator
): Game(simulator) {
    private var started = false
    private var frozen = false

    private var currentLocationPattern = config.locationPattern
    private val occupiedScreens = mutableMapOf<Point, BWGameClient>()
    private val startTime: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()
    private var currentUptime: String = formatUptime(startTime, TimeSource.Monotonic.markNow())
    private val currentClientVisibleShapes = mutableMapOf<Int, MutableSet<VisibleShape>>()

    init {
        if (!started && config.autoStart) {
            simulationScope.launch {
                startGame()
            }
            heartbeatScope.launch {
                checkClientsStillConnected(config.heartbeatTimeoutMillis)
            }
        }
    }

    fun addClient(name: String, version: Int, screenWidth: Int, screenHeight: Int) {
        // what id will we give the client?
        val client = BWGameClient()
        // convert the gameClient.position to a world coordinate. It's half way across and down for whatever screen it's on
        val nextScreenPoint = findNextUnoccupiedScreen()
        gameClient.position = nextScreenPoint

        super.addClient(gameClient, gameClient.position)
    }

    override fun removeClient(clientId: Int) {
        super.removeClient(clientId)
        // now do BW specific client removal

        // TODO:
        // remove from occupiedScreens
        // send event to all clients there's a CLIENT_CHANGE
        println("stop compiler moaning until the TODO is done")
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

    private suspend fun startGame() {
        var iterationStartTimeMillis: Long
        started = true
        currentUptime = formatUptime(startTime, TimeSource.Monotonic.markNow())

        while(running) {
            iterationStartTimeMillis = System.currentTimeMillis()
            if (!frozen) {
                simulator.step()
                currentClientVisibleShapes.clear()
                currentClientVisibleShapes.putAll(findVisibleItemsByClient())
            }
        }
    }
}
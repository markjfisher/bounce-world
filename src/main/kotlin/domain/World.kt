package domain

import config.WorldConfiguration
import data.ShapeCreator
import geometry.GridPatternGenerator
import geometry.Point
import geometry.bounds
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joml.Vector2f
import simulator.WorldSimulator
import java.util.UUID
import kotlin.random.Random

@Singleton
open class World(
    val config: WorldConfiguration
) {
    private val simulationScope = CoroutineScope(Dispatchers.Default)

    private val clients = mutableMapOf<String, GameClient>()
    private val occupiedScreens = mutableMapOf<Point, GameClient>()

    val shapes = ShapeCreator.createShapes()
    private val shapesById = shapes.associateBy { it.id }
    private val shapesByLength = shapes.groupBy { it.sideLength }

    private var isStarted = false
    private var stopped = false

    val currentClientVisibleShapes = mutableMapOf<String, MutableSet<VisibleShape>>()

    val simulator = WorldSimulator(width = config.width, height = config.height, scalingFactor = config.scalingFactor)

    init {
//        val newBodies = createBodies(0, 0, 0, listOf(5, 3, 3, 2, 2, 2, 1, 1, 1, 1))
//        val newBodies = createBodies(0, 0, 0, listOf(5, 3, 3, 2, 2, 2))
        val newBodies = createBodies(0,0, 0, listOf(5, 5))
//        val newBodies = createBodies(0,0, 0, List(30) { 1 } + List(10) { 2 } + List(5) { 3 } + List(2) { 5 })
        simulator.addBodies(newBodies)
    }

    private suspend fun startSimulation() {
        isStarted = true
        while (!stopped) {
            simulator.step()
//            println("bodies:")
//            simulator.bodies.forEach { b ->
//                println("${b.id}: shape: ${b.shapeId}, pos: ${b.position}, vel: ${b.velocity}")
//            }

            currentClientVisibleShapes.clear()
            currentClientVisibleShapes.putAll(simulator.findVisibleShapesByClient(clients.values.toList()))
            delay(500)
        }
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
                startSimulation()
            }
        }
    }

    fun createClient(gameClientInfo: GameClientInfo): GameClient {
        val client = GameClient(
            id = UUID.randomUUID().toString().substring(0, 8),
            name = gameClientInfo.name,
            version = gameClientInfo.version,
            screenSize = gameClientInfo.screenSize,
            viewWidth = config.width,
            viewHeight = config.height,
        )
        addClient(client)
        return client
    }

    fun at(point: Point): GameClient? = occupiedScreens[point]
    fun getClient(id: String): GameClient? = clients[id]

    private fun createRandomBodyWithShape(id: Int, shapeId: Int, offsetX: Int, offsetY: Int): Body {
        val shape = shapesById[shapeId]!!
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
        createRandomBodyWithShape(startId + i, shapesByLength[size]!!.random().id, offsetX, offsetY)
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

}
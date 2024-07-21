package domain

import data.ShapeCreator
import geometry.GridPatternGenerator
import geometry.Point
import geometry.bounds
import jakarta.inject.Singleton
import org.joml.Vector2f
import simulator.WorldSimulator
import kotlin.random.Random

@Singleton
open class World {
    private val clients = mutableMapOf<String, GameClient>()
    private val occupiedScreens = mutableMapOf<Point, GameClient>()

    private val shapes = ShapeCreator.createShapes()
    private val shapesById = shapes.groupBy { it.id }
    private val shapesByLength = shapes.groupBy { it.sideLength }

    private val simulator = WorldSimulator(
        width = SCREEN_WIDTH,
        height = SCREEN_HEIGHT,
        scalingFactor = 4,
        bodies = createBodies(0, 0, listOf(5, 3, 3, 2, 2, 2, 1, 1, 1, 1)).toMutableList(),
        shapes = shapes.toMutableList()
    )

    open fun addClient(gameClient: GameClient) {
        val nextPoint = findNextUnoccupiedScreen()
        gameClient.position = nextPoint
        clients[gameClient.id] = gameClient
        occupiedScreens[nextPoint] = gameClient
    }

    fun at(point: Point): GameClient? = occupiedScreens[point]
    fun getClient(id: String): GameClient? = clients[id]

    private fun createRandomBodyWithShape(shapeId: Int, offsetX: Int, offsetY: Int) = Body(
        // id = id,
        // position is a random location: [(offsetX to offsetX + screenWidth), (offsetY to offsetY + screenHeight)]
        position = Vector2f(
            Random.nextFloat() * SCREEN_WIDTH + offsetX,
            Random.nextFloat() * SCREEN_HEIGHT + offsetY
        ),
        // velocity is a random direction: [(-1 to 1), (-1 to 1)]
        velocity = Vector2f(
            Random.nextFloat() * 2 - 1f,
            Random.nextFloat() * 2 - 1f
        ),
        shapeId = shapeId
    )

    // generates new bodies within a screen, adding the offsets given to position, the world simulator will fit them as close as it can to their position
    private fun createBodies(offsetX: Int, offsetY: Int, sizes: List<Int>): List<Body> = sizes.map { size ->
        // find a random shape with the given size and create a body from it
        createRandomBodyWithShape(shapesByLength.getOrDefault(size, shapesByLength[1]!!).random().id, offsetX, offsetY)
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
package domain

import geometry.GridPatternGenerator
import geometry.Point
import geometry.bounds
import jakarta.inject.Singleton
import simulator.WorldSimulator

@Singleton
open class World {
    private val clients = mutableMapOf<String, GameClient>()
    private val occupiedPoints = mutableMapOf<Point, GameClient>()

    // screens of 40x20 and 32x16 have good mappings down from these sizes
    // The world size will be its boundary * these values.
    private val tileWidth = 160
    private val tileHeight = 80

    private val simulator = WorldSimulator(0, 0, mutableListOf())

    // calculate a client's position within the whole world

    // generate points

    // define shapes the client should draw, the client will get these when registering, and store them. We can then present animations maybe.
    // each shape will be indexed, and only those indices will be passed to client for movement data, so it can draw the screen but with minimal data.
    // the client will be told 10 moves ahead, changes to the world that alter that will only occur on the tick.. maybe

    open fun addClient(gameClient: GameClient) {
        val nextPoint = findNextUnoccupiedPoint()
        gameClient.position = nextPoint
        clients[gameClient.id] = gameClient
        occupiedPoints[nextPoint] = gameClient
    }

    fun at(point: Point): GameClient? = occupiedPoints[point]
    fun getClient(id: String): GameClient? = clients[id]

    private fun findNextUnoccupiedPoint(): Point {
        // walk the sequence of grid points until we find one not in occupiedPoints.
        // This will allow clients to be removed from the world, and replaced by new joiners
        val spiralPoints = GridPatternGenerator().generate().iterator()
        while (spiralPoints.hasNext()) {
            val point = spiralPoints.next()
            if (!occupiedPoints.containsKey(point)) {
                return point
            }
        }
        throw IllegalStateException("Unable to find next unoccupied point.")
    }

    fun removeClient(id: String) {
        val client = getClient(id) ?: return
        clients.remove(client.id)
        val entriesForClient = occupiedPoints.filterValues { c -> c.id == id }
        if (entriesForClient.isNotEmpty()) { occupiedPoints.remove(entriesForClient.keys.first()) }
    }

    fun worldBoundary(): Point {
        if (occupiedPoints.isEmpty()) return Point(0, 0)
        return occupiedPoints.keys.bounds().second + Point(1, 1)
    }

}
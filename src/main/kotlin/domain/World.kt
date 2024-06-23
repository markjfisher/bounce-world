package domain

import geometry.Point
import geometry.SpiralGenerator
import jakarta.inject.Singleton

@Singleton
open class World {
    private val clients = mutableMapOf<String, GameClient>()
    private val occupiedPoints = mutableMapOf<Point, GameClient>()

    open fun addClient(gameClient: GameClient) {
        val nextPoint = findNextUnoccupiedPoint()
        gameClient.position = nextPoint
        clients[gameClient.id] = gameClient
        occupiedPoints[nextPoint] = gameClient
    }

    fun at(point: Point): GameClient? = occupiedPoints[point]
    fun getClient(id: String): GameClient? = clients[id]

    private fun findNextUnoccupiedPoint(): Point {
        // walk the sequence of spiral points until we find one not in occupiedPoints.
        // This will allow clients to be removed from the world, and replaced by new joiners
        val spiralPoints = SpiralGenerator().generate().iterator()
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
}
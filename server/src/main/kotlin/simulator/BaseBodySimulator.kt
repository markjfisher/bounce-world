package simulator

import config.WorldConfig
import domain.Body
import geometry.SpiralGenerator
import logger
import org.joml.Vector2f

abstract class BaseBodySimulator(config: WorldConfig): WorldSimulator {
    override var width: Int = config.width
    override var height: Int = config.height
    override var currentStep: Int = 0
    override val collisions: MutableSet<Int> = mutableSetOf()
    override val bodies: MutableList<Body> = mutableListOf()

    val stepTime = 1f / config.updatesPerSecond
    override fun reset() { bodies.clear() }

    abstract fun calculateDistance(a: Body, b: Body): Float

    override fun addBodies(bodies: List<Body>) {
        // attempt to add the newBodies to the simulator, adjusting them to fit into empty spaces closest to their intended locations
        bodies.forEach { b ->
            val movedBody = moveBody(b)
            if (movedBody == null) {
                logger.warn("ERROR: could not fit body $b onto grid, skipping to next.")
            } else {
                this.bodies.add(movedBody)
            }
        }
    }

    private fun moveBody(b: Body): Body? {
        if (!isOverlapping(b)) return b

        var testedPoints = 0
        val bodyPos = Vector2f(b.position)
        val mutB = Body(position = Vector2f(b.position), velocity = Vector2f(b.velocity), mass = b.mass, id = b.id, radius = b.radius, shapeId = b.shapeId)

        // spiral out from our current position until we hit a point that does not intersect with anything on the grid
        val spiralPoints = SpiralGenerator().generate().iterator()
        spiralPoints.next() // skip the first point, it's 0,0 which won't generate a change

        // ensure we don't accidentally loop forever by checking we don't do more than every point in the grid.
        while (testedPoints < width * height) {
            val offset = spiralPoints.next()
            val testPosition = boundVector(Vector2f(bodyPos).add(offset.x.toFloat(), offset.y.toFloat()))
            // change the body's position by the spiral offset, which circles the original point
            mutB.position.set(testPosition)
            if (!isOverlapping(mutB)) {
                return mutB
            }
            testedPoints++
        }
        return null
    }

    fun boundVector(v: Vector2f): Vector2f {
        // Wrap the position to the world dimensions
        val wrappedX = if (v.x < 0) {
            (v.x % width + width) % width
        } else {
            v.x % width
        }
        val wrappedY = if (v.y < 0) {
            (v.y % height + height) % height
        } else {
            v.y % height
        }

        return Vector2f(wrappedX, wrappedY)
    }

    private fun isOverlapping(b: Body): Boolean = bodies.any { otherBody ->
        isOverlapping(otherBody, b)
    }

    private fun isOverlapping(a: Body, b: Body): Boolean {
        val distanceApart = calculateDistance(a, b)
        val sumOfRadii = (a.radius + b.radius)
        return distanceApart < sumOfRadii
    }
}

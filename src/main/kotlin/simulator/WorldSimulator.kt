package simulator

import config.WorldConfiguration
import domain.Body
import domain.CollisionEvent
import geometry.SpiralGenerator
import jakarta.inject.Singleton
import maths.QuadraticSolver
import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("DuplicatedCode")
@Singleton
data class WorldSimulator(
    private val config: WorldConfiguration
): BodySimulator {
    private var worldWidth = config.width
    private var worldHeight = config.height
    private var scalingFactor = config.scalingFactor
    private var isWrapping = config.enableWrapping
    val bodies: MutableList<Body> = mutableListOf()
    private var currentStep: Int = 0

    // the ids of bodies that collided this step, so we can indicate to the client their screen had a collision for any effects they want to provide
    private val collisions: MutableSet<Int> = mutableSetOf()

    // UPS timings
    private val stepTime = 1f / config.updatesPerSecond

    override fun setWidth(width: Int) { this.worldWidth = width }
    override fun setHeight(height: Int) { this.worldHeight = height }
    override fun height() = this.worldHeight
    override fun width() = this.worldWidth
    override fun collisions() = this.collisions
    override fun bodies() = this.bodies
    override fun currentStep() = currentStep
    override fun isWrapping() = isWrapping

    private fun boundVector(v: Vector2f): Vector2f {
        // Wrap the position to the world dimensions
        val wrappedX = if (v.x < 0) {
            (v.x % worldWidth + worldWidth) % worldWidth
        } else {
            v.x % worldWidth
        }
        val wrappedY = if (v.y < 0) {
            (v.y % worldHeight + worldHeight) % worldHeight
        } else {
            v.y % worldHeight
        }

        return Vector2f(wrappedX, wrappedY)
    }

    // find the world coordinates of the 4 corner points a body covers, with scaling of the world
    private fun moveBody(b: Body): Body? {
        if (!isOverlapping(b)) return b

        var testedPoints = 0
        val bodyPos = Vector2f(b.position)
//        val mutB = b.copy(position = Vector2f(b.position), velocity = Vector2f(b.velocity))
        val mutB = Body(position = Vector2f(b.position), velocity = Vector2f(b.velocity), mass = b.mass, id = b.id, radius = b.radius, shapeId = b.shapeId)

        // spiral out from our current position until we hit a point that does not intersect with anything on the grid
        val spiralPoints = SpiralGenerator().generate().iterator()
        spiralPoints.next() // skip the first point, it's 0,0 which won't generate a change

        // ensure we don't accidentally loop forever by checking we don't do more than every point in the grid.
        while (testedPoints < worldWidth * worldHeight) {
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

    private fun isOverlapping(a: Body, b: Body): Boolean {
        val distanceApart = calculateDistance(a, b)
        val sumOfRadii = (a.radius + b.radius) * scalingFactor
        return distanceApart < sumOfRadii
    }

    private fun isOverlapping(b: Body): Boolean = bodies.any { otherBody ->
        isOverlapping(otherBody, b)
    }

    override fun addBodies(bodies: List<Body>) {
        // attempt to add the newBodies to the simulator, adjusting them to fit into empty spaces closest to their intended locations
        bodies.forEach { b ->
            val movedBody = moveBody(b)
            if (movedBody == null) {
                println("ERROR: could not fit body $b onto grid, skipping to next.")
            } else {
                this.bodies.add(movedBody)
            }
        }
    }

    override fun step() {
        collisions.clear()
        bodies.forEach { body ->
            // distance = speed * time
            val delta = Vector2f(body.velocity).mul(stepTime)
            body.intendedPosition.set(body.position).add(delta)
        }

        var collisionsDetected: Boolean
        val maxIterations = 10 // Limit the number of iterations to prevent infinite loops
        var currentIteration = 0

        do {
            collisionsDetected = false
            bodies.forEachIndexed { i, a ->
                for (j in i + 1 until bodies.size) {
                    val b = bodies[j]
                    val collisionTime = calculateCollisionTime(a, b)
                    if (collisionTime != null) {
                        resolveCollision(a, b, collisionTime)
                        collisionsDetected = true
                        collisions.add(a.id)
                        collisions.add(b.id)
                    }
                }
            }
            currentIteration++
        } while (collisionsDetected && currentIteration < maxIterations)

        bodies.forEach { body ->
            body.position.set(if (isWrapping) boundVector(body.intendedPosition) else body.intendedPosition)
        }
        // bound the step number to a byte value
        if (currentStep++ > 255) currentStep = 0
    }

    private fun calculateWrappedDistance(a: Float, b: Float, maxDistance: Float): Float {
        val directDistance = abs(a - b)
        val wrappedDistance = min(directDistance, abs(maxDistance - directDistance))
        return wrappedDistance
    }

    fun calculateDistance(a: Body, b: Body): Float {
        val xDistance = calculateWrappedDistance(a.position.x, b.position.x, worldWidth.toFloat())
        val yDistance = calculateWrappedDistance(a.position.y, b.position.y, worldHeight.toFloat())
        val distance = sqrt(xDistance.pow(2) + yDistance.pow(2))
        return distance
    }

    fun findClosestWrappedPosition(a: Vector2f, b: Vector2f): Vector2f {
        val w = worldWidth.toFloat()
        val h = worldHeight.toFloat()
        // Define the shifts for the 8 surrounding positions plus the original (0,0) position
        val shifts = listOf(
            Pair(0f, 0f), // Original
            Pair(-w, 0f), // W
            Pair(w, 0f), // E
            Pair(0f, -h), // N
            Pair(0f, h), // S
            Pair(-w, -h), // NW
            Pair(w, -h), // NE
            Pair(-w, h), // SW
            Pair(w, h) // SE
        )

        val (dx, dy) = shifts.minByOrNull { (dx, dy) ->
            val wrappedB = Vector2f(b.x + dx, b.y + dy)
            a.distance(wrappedB)
        } ?: Pair(0f, 0f)

        return Vector2f(b.x + dx, b.y + dy)
    }

    // Will the 2 bodies collide in the current 1s timeframe?
    fun calculateCollisionTimes(a: Body, b: Body): List<Float> {
        val bClosestPosition = findClosestWrappedPosition(a.position, b.position)
        // this will be bound later
        b.position.set(bClosestPosition)

        val px = a.position.x - b.position.x
        val py = a.position.y - b.position.y
        val vx = a.velocity.x - b.velocity.x
        val vy = a.velocity.y - b.velocity.y

        val qa = vx * vx + vy * vy
        val qb = 2 * (px * vx + py * vy)
        // The shape data is based on a 40x20 screen, not the world size, so shapes have to be scaled up to the world sizes by the scaling factor, e.g. 160x80 means scalingFactor = 4x
        val radii = scalingFactor * (a.radius + b.radius)
        val qc = px * px + py * py - radii * radii

        val quadraticSolver = QuadraticSolver(qa, qb, qc)
        val roots = quadraticSolver.solveRealRoots()
        // look for intercept time in range 0 to stepTime second
        val validRoots = roots.filter { it >= 0.0f && it < stepTime }
        // println("a: $a, b: $b, validRoots: $validRoots")
        return validRoots
    }

    private fun calculateCollisionTime(a: Body, b: Body): Float? {
        return calculateCollisionTimes(a, b).minOrNull()
    }

    private fun resolveCollision(a: Body, b: Body, collisionTime: Float) {
        val acp = Vector2f(a.position).add(Vector2f(a.velocity).mul(stepTime).mul(collisionTime))
        val bcp = Vector2f(b.position).add(Vector2f(b.velocity).mul(stepTime).mul(collisionTime))
        val collisionNormal = Vector2f(bcp).sub(acp).normalize()

        val relativeVelocity = Vector2f(b.velocity).sub(a.velocity)
        val velocityAlongNormal = relativeVelocity.dot(collisionNormal)

        // Do not resolve if velocities are separating, nothing has changed, the normal position will be calculated
        if (velocityAlongNormal > 0) return

        // give a bit of randomness to the elasticity, this is 0.9 to + 1.1, so on average it will keep the energy in the total system
        // but some collisions will kick off, some will absorb
        // val e = 1f + Random.nextFloat() / 5f - 0.1f

        // Calculate restitution (elastic collision)
        // Coefficient of restitution; e = 1 for perfectly elastic collisions
        val e = 1f

        // Calculate impulse scalar
        val j = -(1 + e) * velocityAlongNormal / (1 / a.mass + 1 / b.mass)

        // Apply impulse.
        val impulse = Vector2f(collisionNormal).mul(j)

        // calculate the new velocity of each body
        a.velocity.add(Vector2f(impulse).mul(1 / a.mass).negate())
        b.velocity.add(Vector2f(impulse).mul(1 / b.mass))

        // work out how much time it would be travelling from the CP with its new velocity in the time remaining
        val timeRemaining = stepTime - collisionTime

        val distanceAAfterCollision = Vector2f(a.velocity).mul(stepTime).mul(timeRemaining)
        val distanceBAfterCollision = Vector2f(b.velocity).mul(stepTime).mul(timeRemaining)

        val finalPositionA = Vector2f(acp).add(distanceAAfterCollision)
        val finalPositionB = Vector2f(bcp).add(distanceBAfterCollision)

        // Update the intended positions to the final positions
        a.intendedPosition.set(finalPositionA)
        b.intendedPosition.set(finalPositionB)
    }

    fun calculateInitialPotentialCollisions(bodies: List<Body>): List<CollisionEvent> {
        val potentialCollisions = mutableListOf<CollisionEvent>()

        // Calculate potential body-to-body collisions
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val body1 = bodies[i]
                val body2 = bodies[j]

                val time = calculateCollisionTime(body1, body2)
                if (time != null && time >= 0) {
                    potentialCollisions.add(CollisionEvent(time, body1, body2))
                }
            }
        }

        // Calculate potential body-to-wall collisions
        bodies.forEach { body ->
            calculateWallCollisionTimes(body).forEach { time ->
                potentialCollisions.add(CollisionEvent(time, body))
            }
        }

        return potentialCollisions
    }

    private fun calculateWallCollisionTimes(body: Body): List<Float> {
        val times = mutableListOf<Float>()

        // Calculate time to collide with the left wall (x = 0)
        if (body.velocity.x < 0) { // Moving towards the left wall
            val timeToLeftWall = (body.radius - body.position.x) / body.velocity.x
            if (timeToLeftWall >= 0f && timeToLeftWall < stepTime) {
                times.add(timeToLeftWall)
            }
        }

        // Calculate time to collide with the right wall (x = width)
        if (body.velocity.x > 0) { // Moving towards the right wall
            val timeToRightWall = (worldWidth - body.radius - body.position.x) / body.velocity.x
            if (timeToRightWall >= 0f && timeToRightWall < stepTime) {
                times.add(timeToRightWall)
            }
        }

        // Calculate time to collide with the top wall (y = 0)
        if (body.velocity.y < 0) { // Moving towards the top wall
            val timeToTopWall = (body.radius - body.position.y) / body.velocity.y
            if (timeToTopWall >= 0f && timeToTopWall < stepTime) {
                times.add(timeToTopWall)
            }
        }

        // Calculate time to collide with the bottom wall (y = height)
        if (body.velocity.y > 0) { // Moving towards the bottom wall
            val timeToBottomWall = (worldHeight - body.radius - body.position.y) / body.velocity.y
            if (timeToBottomWall >= 0f && timeToBottomWall < stepTime) {
                times.add(timeToBottomWall)
            }
        }

        return times
    }

}
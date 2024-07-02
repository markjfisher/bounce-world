package simulator

import domain.Body
import maths.QuadraticSolver
import org.joml.Vector2f

data class WorldSimulator(
    var width: Int,
    var height: Int,
    val bodies: MutableList<Body>
) {
    fun step(isWrapping: Boolean = true) {
        bodies.forEach { body ->
            body.intendedPosition.set(body.position).add(body.velocity)
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
                    }
                }
            }
            currentIteration++
        } while (collisionsDetected && currentIteration < maxIterations)

        bodies.forEach { body ->
            if (isWrapping) {
                // Wrap the position to the world dimensions
                val wrappedX = if (body.intendedPosition.x < 0) {
                    (body.intendedPosition.x % width + width) % width
                } else {
                    body.intendedPosition.x % width
                }
                val wrappedY = if (body.intendedPosition.y < 0) {
                    (body.intendedPosition.y % height + height) % height
                } else {
                    body.intendedPosition.y % height
                }

                body.position.set(Vector2f(wrappedX, wrappedY))
            } else {
                body.position.set(body.intendedPosition)
            }
        }

    }

    private fun calculateCollisionTime(a: Body, b: Body): Float? {
        val px = a.position.x - b.position.x
        val py = a.position.y - b.position.y
        val vx = a.velocity.x - b.velocity.x
        val vy = a.velocity.y - b.velocity.y

        val qa = vx * vx + vy * vy
        val qb = 2 * (px * vx + py * vy)
        val radii = (a.shape.sideLength + b.shape.sideLength) / 2f
        val qc = px * px + py * py - radii * radii

        val quadraticSolver = QuadraticSolver(qa, qb, qc)
        val roots = quadraticSolver.solveRealRoots()

        // Check if any of the roots are within the step duration (0 to 1)
        val validRoots = roots.filter { it >= 0.0f && it < 1.0f }
        return validRoots.minOrNull()
    }

    private fun resolveCollision(a: Body, b: Body, collisionTime: Float) {
        val acp = Vector2f(a.position).add(Vector2f(a.velocity).mul(collisionTime))
        val bcp = Vector2f(b.position).add(Vector2f(b.velocity).mul(collisionTime))
        val collisionNormal = Vector2f(bcp).sub(acp).normalize()

        val relativeVelocity = Vector2f(b.velocity).sub(a.velocity)
        val velocityAlongNormal = relativeVelocity.dot(collisionNormal)

        // Do not resolve if velocities are separating, nothing has changed, the normal position will be calculated
        if (velocityAlongNormal > 0) return

        // Calculate restitution (elastic collision)
        val e = 1.0f // Coefficient of restitution; e = 1 for perfectly elastic collisions

        // Calculate impulse scalar
        val j = -(1 + e) * velocityAlongNormal / (1 / a.shape.mass + 1 / b.shape.mass)

        // Apply impulse.
        val impulse = Vector2f(collisionNormal).mul(j)

        // calculate the new velocity of each body
        a.velocity.add(Vector2f(impulse).mul(1 / a.shape.mass).negate())
        b.velocity.add(Vector2f(impulse).mul(1 / b.shape.mass))

        // time in a step is 1s by design, work out how much time it would be travelling from the CP with its new velocity
        val timeRemaining = 1.0f - collisionTime

        val distanceAAfterCollision = Vector2f(a.velocity).mul(timeRemaining)
        val distanceBAfterCollision = Vector2f(b.velocity).mul(timeRemaining)

        val finalPositionA = Vector2f(acp).add(distanceAAfterCollision)
        val finalPositionB = Vector2f(bcp).add(distanceBAfterCollision)

        // Update the intended positions to the final positions
        a.intendedPosition.set(finalPositionA)
        b.intendedPosition.set(finalPositionB)
    }
}
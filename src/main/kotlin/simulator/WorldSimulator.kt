package simulator

import domain.Body
import domain.Shape
import geometry.Point
import geometry.SpiralGenerator
import geometry.points
import maths.QuadraticSolver
import org.joml.Vector2f

data class WorldSimulator(
    var width: Int,
    var height: Int,
    // this only affects the shape's radius as the shape data is based on 40x20 screen, not 160x80 world coords
    val scalingFactor: Int,
    val bodies: MutableList<Body>,
    val shapes: Map<Int, Shape>
) {
    private fun boundPoint(p: Point): Point {
        val wrappedX = if (p.x < 0) {
            (p.x % width + width) % width
        } else {
            p.x % width
        }
        val wrappedY = if (p.y < 0) {
            (p.y % height + height) % height
        } else {
            p.y % height
        }
        return Point(wrappedX, wrappedY)
    }

    private fun boundVector(v: Vector2f): Vector2f {
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

        v.x = wrappedX
        v.y = wrappedY
        return v
    }

    // calculates all points in the entire grid that are covered by bodies
    fun gridPoints(): Set<Point> {
        val allPoints = mutableSetOf<Point>()
        bodies.forEach { b ->
            val coveredPoints = bodyPoints(b)

            // validate non of these are in allPoints already, which would mean bodies are overlapping
            val overlappingPoints = coveredPoints.intersect(allPoints.toSet())
            if (overlappingPoints.isNotEmpty()) {
                println("ERROR: overlapping bodies at: $overlappingPoints, body: $b")
            }

            allPoints.addAll(coveredPoints)
        }
        return allPoints
    }

    private fun bodyPoints(b: Body): List<Point> {
        val centre = Point(b.position.x.toInt(), b.position.y.toInt())
        val sideLength = shapes[b.shapeId]!!.sideLength
        // calculate the offsets to the centre point for grid positions this body covers
        val offsets = when {
            // even width needs offset of -[(n/2 -1),(n/2 -1)], +[n/2, n/2]
            sideLength.mod(2) == 0 -> Pair(
                Point(sideLength / 2 - 1, sideLength / 2 - 1),
                Point(sideLength / 2, sideLength / 2)
            )
            // odd width needs offset of +/- [(n-1)/2]
            else -> Pair(
                Point((sideLength - 1) / 2, (sideLength - 1) / 2),
                Point((sideLength - 1) / 2, (sideLength - 1) / 2)
            )
        }
        // find the extreme points from centre with these offsets
        val topLeft = centre - offsets.first * scalingFactor
        val bottomRight = centre + offsets.second * scalingFactor

        // and calculate all the points in this box
        return Pair(topLeft, bottomRight).points().map { p ->
            boundPoint(p)
        }.toList()
    }

    private fun moveBody(b: Body): Body? {
        if (!isOverlapping(b)) return b

        var testedPoints = 0
        val bodyPos = Vector2f(b.position)
        val mutB = Body(position = Vector2f(b.position), velocity = Vector2f(b.velocity), shapeId = b.shapeId)

        // spiral out from our current position until we hit a point that does not intersect with anything on the grid
        val spiralPoints = SpiralGenerator().generate().iterator()
        spiralPoints.next() // skip the first point, it's 0,0 which won't generate a change

        // ensure we don't accidentally loop forever by checking we don't do more than every point in the grid.
        while (spiralPoints.hasNext() && testedPoints < width * height) {
            val offset = boundPoint(spiralPoints.next())
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

    private fun isOverlapping(b: Body): Boolean {
        val radiusB = shapes[b.shapeId]!!.sideLength / 2.0f

        // Check if there's any body in the list that overlaps with b
        return bodies.any { otherBody ->
            val radiusOther = shapes[otherBody.shapeId]!!.sideLength / 2.0f
            val centerDistance = b.position.distance(otherBody.position)
            centerDistance <= (radiusB + radiusOther)
        }
    }

    fun addBodies(newBodies: List<Body>) {
        // attempt to add the newBodies to the simulator, adjusting them to fit into empty spaces closest to their intended locations
        newBodies.forEach { b ->
            val movedBody = moveBody(b)
            if (movedBody == null) {
                println("ERROR: could not fit body $b onto grid, skipping to next.")
            } else {
                bodies.add(movedBody)
            }
        }
    }

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
        // The shape data is based on a 40x20 screen, not the world size, so shapes have to be scaled up to the world sizes by the scaling factor, e.g. 160x80 means scalingFactor = 4x
        val radii = (shapes[a.shapeId]!!.sideLength * scalingFactor + shapes[b.shapeId]!!.sideLength * scalingFactor) / 2f
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
        val j = -(1 + e) * velocityAlongNormal / (1 / shapes[a.shapeId]!!.mass + 1 / shapes[b.shapeId]!!.mass)

        // Apply impulse.
        val impulse = Vector2f(collisionNormal).mul(j)

        // calculate the new velocity of each body
        a.velocity.add(Vector2f(impulse).mul(1 / shapes[a.shapeId]!!.mass).negate())
        b.velocity.add(Vector2f(impulse).mul(1 / shapes[b.shapeId]!!.mass))

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
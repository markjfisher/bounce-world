package simulator

import domain.Body
import domain.GameClient
import domain.Shape
import domain.VisibleShape
import domain.World.Companion.SCREEN_HEIGHT
import domain.World.Companion.SCREEN_WIDTH
import geometry.Point
import geometry.SpiralGenerator
import maths.QuadraticSolver
import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class WorldSimulator(
    var width: Int,
    var height: Int,
    // this only affects the shape's radius as the shape data is based on 40x20 screen, not 160x80 world coords
    val scalingFactor: Int,
    val bodies: MutableList<Body>,
    val shapes: Map<Int, Shape>,
    val screenWidth: Int = SCREEN_WIDTH,
    val screenHeight: Int = SCREEN_HEIGHT,
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

        return Vector2f(wrappedX, wrappedY)
    }

    // find the world coordinates of the 4 corner points a body covers
    fun bodyCorners(b: Body): List<Point> {
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
        val topRight = Point(bottomRight.x, topLeft.y)
        val bottomLeft = Point(topLeft.x, bottomRight.y)

        return listOf(topLeft, topRight, bottomLeft, bottomRight).map { p -> boundPoint(p) }
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
        val sumOfRadii = (shapes[a.shapeId]!!.sideLength + shapes[b.shapeId]!!.sideLength) / 2f * scalingFactor
        return distanceApart < sumOfRadii
    }

    private fun isOverlapping(b: Body): Boolean = bodies.any { otherBody ->
        isOverlapping(otherBody, b)
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
            body.position.set(if (isWrapping) boundVector(body.intendedPosition) else body.intendedPosition)
        }

    }

    private fun calculateWrappedDistance(a: Float, b: Float, maxDistance: Float): Float {
        val directDistance = abs(a - b)
        val wrappedDistance = min(directDistance, abs(maxDistance - directDistance))
        return wrappedDistance
    }

    fun calculateDistance(a: Body, b: Body): Float {
        val xDistance = calculateWrappedDistance(a.position.x, b.position.x, width.toFloat())
        val yDistance = calculateWrappedDistance(a.position.y, b.position.y, height.toFloat())
        val distance = sqrt(xDistance.pow(2) + yDistance.pow(2))
        return distance
    }

    fun findClosestWrappedPosition(a: Vector2f, b: Vector2f): Vector2f {
        val w = width.toFloat()
        val h = height.toFloat()
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
        val radii = scalingFactor * (shapes[a.shapeId]!!.sideLength + shapes[b.shapeId]!!.sideLength) / 2f
        val qc = px * px + py * py - radii * radii

        val quadraticSolver = QuadraticSolver(qa, qb, qc)
        val roots = quadraticSolver.solveRealRoots()
        // look for intercept time in range 0 to 1 second
        val validRoots = roots.filter { it >= 0.0f && it < 1.0f }
        // println("a: $a, b: $b, validRoots: $validRoots")
        return validRoots
    }

    private fun calculateCollisionTime(a: Body, b: Body): Float? {
        return calculateCollisionTimes(a, b).minOrNull()
    }

    private fun resolveCollision(a: Body, b: Body, collisionTime: Float) {
        val acp = Vector2f(a.position).add(Vector2f(a.velocity).mul(collisionTime))
        val bcp = Vector2f(b.position).add(Vector2f(b.velocity).mul(collisionTime))
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

    // find every VisibleShape for every client
    @Suppress("LocalVariableName")
    fun findVisibleShapesByClient(clients: List<GameClient>): Map<String, MutableSet<VisibleShape>> {

        fun clientIdThatOwns(p: Point): String {
            return clients.first { c ->
                p.within(c.worldBounds)
            }.id
        }

        // initialise the returned map
        val visibleShapesByClient = mutableMapOf<String, MutableSet<VisibleShape>>()
        clients.forEach { client ->
            visibleShapesByClient[client.id] = mutableSetOf()
        }

        bodies.forEach { body ->
//            val bodyCentre = Point(body.position.x.toInt(), body.position.y.toInt())
            val bodyWidth = shapes[body.shapeId]!!.sideLength

            val corners = bodyCorners(body)
            // look at the corners of the body in order (NW, NE, SW, SE), and for the single client it is in, work out what the visibleshape is for it.
            // this may end up with 4 visibleshapes even for the same client if it wraps in a way to show it that way (there would only be a single client)
            // Equally it could be shown in 4 different clients if there are 4 of them and the body is in a corner between all 4.

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

            // add the visibleshape to the client's list. dupes will be removed, and this also caters for both wrapping and no wrapping
            visibleShapesByClient[clientIdThatOwns(cNW)]!!.add(VisibleShape(body.shapeId, centre1))
            visibleShapesByClient[clientIdThatOwns(cNE)]!!.add(VisibleShape(body.shapeId, centre2))
            visibleShapesByClient[clientIdThatOwns(cSW)]!!.add(VisibleShape(body.shapeId, centre3))
            visibleShapesByClient[clientIdThatOwns(cSE)]!!.add(VisibleShape(body.shapeId, centre4))

//            if (centre1 != bodyCentre) {
//                visibleShapesByClient[clientIdThatOwns(cNW)]!!.add(VisibleShape(body.shapeId, centre1))
//            }
//
//            if (centre2 != bodyCentre) {
//                visibleShapesByClient[clientIdThatOwns(cNE)]!!.add(VisibleShape(body.shapeId, centre2))
//            }
//
//            if (centre3 != bodyCentre) {
//                visibleShapesByClient[clientIdThatOwns(cSW)]!!.add(VisibleShape(body.shapeId, centre3))
//            }
//
//            if (centre4 != bodyCentre) {
//                visibleShapesByClient[clientIdThatOwns(cSE)]!!.add(VisibleShape(body.shapeId, centre4))
//            }

        }
        return visibleShapesByClient
    }

}
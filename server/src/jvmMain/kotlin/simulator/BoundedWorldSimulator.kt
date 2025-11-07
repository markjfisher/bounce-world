package simulator

import config.WorldConfig
import data.Quadtree
import domain.Body
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.locks.withLock
import org.joml.Vector2f

@Suppress("DuplicatedCode")
data class BoundedWorldSimulator(
    private val config: WorldConfig,
): BaseBodySimulator(config) {

    @OptIn(InternalAPI::class)
    override fun doStepLocked() {
        val checkedPairs = HashSet<Long>(bodies.size * 2)
        val quadtree = Quadtree(width, height, 1, 6)
        collisions.clear()
        for (body in bodies) {
            // create a rectangle for the location of the current body, make it slightly larger than a box covering the radius of the body
            val bound = Vector2f(body.radius, body.radius).mul(1.05f)
            val ul = Vector2f(body.position).sub(bound)
            val lr = Vector2f(body.position).add(bound)
            quadtree.insert(body.id, ul.x, ul.y, lr.x, lr.y)
        }

        // create a faster lookup
        val byId = HashMap<Int, Body>(bodies.size)
        for (b in bodies) byId[b.id] = b

        // now loop over all bodies, and find their closest potential collisions
        for (bodyA in bodies) {
            val bound = Vector2f(bodyA.radius * 2f, bodyA.radius * 2f)
            val ul = Vector2f(bodyA.position).sub(bound)
            val lr = Vector2f(bodyA.position).add(bound)
            val neighbours = quadtree.queryWithIds(ul.x, ul.y, lr.x, lr.y)
                .filterNot { it.second == bodyA.id }

            for ((_, bodyId) in neighbours) {
                if (bodyId == bodyA.id) continue
                val bodyB = byId[bodyId] ?: continue

                // pack unordered pair into a single Long to avoid Pair allocs
                val a = bodyA.id
                val b = bodyB.id
                val key = if (a < b) (a.toLong() shl 32) or (b.toLong() and 0xffffffffL)
                          else (b.toLong() shl 32) or (a.toLong() and 0xffffffffL)

                if (checkedPairs.add(key)) {
                    resolveCollision(bodyA, bodyB)
                }
            }
        }

        // engineers eh? "close enough"
        for (body in bodies) {
            update(body)
            edges(body)
        }

    }

    override fun calculateDistance(a: Body, b: Body): Float {
        return Vector2f(a.position).sub(b.position).length()
    }

    private fun resolveCollision(bodyA: Body, bodyB: Body) {
        // This is super simple, and assumes that 2 bodies are not travelling fast enough to be on the other side of
        // each other after the time step.
        // TODO: use quadratic solver to calculate if there's an actual collision in the timestep rather than just looking at their relative distance
        val impactVector = Vector2f(bodyB.position).sub(bodyA.position)
        val d = impactVector.length()
        val sumOfRadii = bodyA.radius + bodyB.radius
        if (d < sumOfRadii) {
            collisions.add(bodyA.id)
            collisions.add(bodyB.id)

            val overlap = d - sumOfRadii
            val dir = Vector2f(impactVector).normalize().mul(overlap * 0.5f)
            // Change the body positions
            bodyA.position.add(dir)
            bodyB.position.sub(dir)

            impactVector.normalize().mul(sumOfRadii)
            val totalMass = bodyA.mass + bodyB.mass
            val deltaV = Vector2f(bodyB.velocity).sub(bodyA.velocity)

            // change the body velocities
            val numerator = deltaV.dot(impactVector)
            val denominator = totalMass * sumOfRadii * sumOfRadii
            val deltaVA = Vector2f(impactVector).mul((2f * bodyB.mass * numerator) / denominator)
            bodyA.velocity.add(deltaVA)

            val deltaVB = Vector2f(impactVector).mul((-2f * bodyA.mass * numerator) / denominator)
            bodyB.velocity.add(deltaVB)
        }
    }

    private fun update(body: Body) {
        val delta = Vector2f(body.velocity).mul(stepTime)
        body.position.add(delta)
    }

    // this is cheap and nasty. it should work out how far it bounces from the wall by
    // looking at the step time and calculating when it hits the wall.
    // It also uses a small delta near the edges to ensure the client should never wrap because of rounding
    // example output to client where there were only 10 shapes:
    /*
        00000000: 6c00 0b01 1405 001c 1101 0d07 0112 1601  l...............
        00000010: 12fe 0020 1500 190a 0107 0e01 1e05 0117  ... ............
        00000020: 1300 060a
     */
    // There were 11 shapes sent, all different data.
    // Looking at the data, there's one shape that has:
    //   01 12 16
    //   01 12 fe
    // so it was reflected in the Y plane, as it was close to an edge.
    // Added a small delta to ensure the object is not within that distance of the edge
    // so rounding doesn't cause it to reflect

    private fun edges(body: Body) {
        if ((body.position.x + body.radius) > (width.toFloat() - EDGE_DELTA)) {
            body.position.x = width - body.radius - EDGE_DELTA
            body.velocity.x *= -1f
        } else if ((body.position.x - body.radius) < EDGE_DELTA) {
            body.position.x = body.radius + EDGE_DELTA
            body.velocity.x *= -1f
        }
        if ((body.position.y + body.radius) > (height.toFloat() - EDGE_DELTA)) {
            body.position.y = height - body.radius - EDGE_DELTA
            body.velocity.y *= -1f
        } else if ((body.position.y - body.radius) < EDGE_DELTA) {
            body.position.y = body.radius + EDGE_DELTA
            body.velocity.y *= -1f
        }
    }

    companion object {
        const val EDGE_DELTA = 0.2f
    }
}
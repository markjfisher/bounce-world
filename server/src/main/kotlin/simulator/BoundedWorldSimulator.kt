package simulator

import config.WorldConfig
import data.Quadtree
import domain.Body
import org.joml.Vector2f

@Suppress("DuplicatedCode")
data class BoundedWorldSimulator(
    private val config: WorldConfig,
): BaseBodySimulator(config) {
    override fun step() {
        val checkedPairs: MutableSet<Pair<Int, Int>> = mutableSetOf()
        val quadtree = Quadtree(width, height, 1, 6)
        collisions.clear()
        bodies.forEach { body ->
            // create a rectangle for the location of the current body, make it slightly larger than a box covering the radius of the body
            val bound = Vector2f(body.radius, body.radius).mul(1.05f)
            val upperLeft = Vector2f(body.position).sub(bound)
            val lowerRight = Vector2f(body.position).add(bound)
            quadtree.insert(body.id, upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y)
        }

        // now loop over all bodies, and find their closest potential collisions
        bodies.forEach { bodyA ->
            val bound = Vector2f(bodyA.radius * 2f, bodyA.radius * 2f)
            val upperLeft = Vector2f(bodyA.position).sub(bound)
            val lowerRight = Vector2f(bodyA.position).add(bound)
            val qNeighbours = quadtree.queryWithIds(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y).filterNot { it.second == bodyA.id }

            qNeighbours.forEach { (_, bodyId) ->
                val bodyB = bodies.find { it.id == bodyId }
                if (bodyB != null) {
                    val pair = if (bodyA.id < bodyB.id) Pair(bodyA.id, bodyB.id) else Pair(bodyB.id, bodyB.id)
                    if (!checkedPairs.contains(pair)) {
                        resolveCollision(bodyA, bodyB)
                        checkedPairs.add(pair)
                    }
                }
            }
        }

        // engineers eh? "close enough"
        bodies.forEach { body ->
            update(body)
            edges(body)
        }
        if (currentStep++ > 255) currentStep = 0
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
    // looking at the step time and calculating when it hits the wall
    private fun edges(body: Body) {
        if (body.position.x > width - body.radius) {
            body.position.x = width - body.radius
            body.velocity.x *= -1f
        } else if (body.position.x < body.radius) {
            body.position.x = body.radius
            body.velocity.x *= -1f
        }
        if (body.position.y > height - body.radius) {
            body.position.y = height - body.radius
            body.velocity.y *= -1f
        } else if (body.position.y < body.radius) {
            body.position.y = body.radius
            body.velocity.y *= -1f
        }
    }

}
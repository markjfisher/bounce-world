package simulator

import config.WorldConfig
import domain.Body
import domain.BodyView
import geometry.SpiralGenerator
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.locks.withLock
import logger
import org.joml.Vector2f
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs

@OptIn(InternalAPI::class)
abstract class BaseBodySimulator(config: WorldConfig): WorldSimulator {
    private val idSeq = AtomicInteger(1)
    override fun nextBodyId(): Int = idSeq.getAndIncrement()

    abstract fun calculateDistance(a: Body, b: Body): Float
    protected abstract fun doStepLocked()

    override var width: Int = config.width
    override var height: Int = config.height
    @Volatile override var currentStep: Int = 0
        protected set

    protected val collisions: MutableSet<Int> = mutableSetOf()
    protected val bodies: MutableList<Body> = mutableListOf()

    override fun collisionsCopy(): Set<Int> = lock.withLock { collisions.toSet() }

    private val lock = ReentrantLock()
    private val pendingAdds = ConcurrentLinkedQueue<Body>()

    override fun <T> withBodiesRead(block: (List<Body>) -> T): T =
        lock.withLock { block(bodies) }

    override fun updateBodies(block: (MutableList<Body>) -> Unit) {
        lock.withLock {
            drainAdds()
            block(bodies)
        }
    }

    override fun bodyCount(): Int = lock.withLock { bodies.size }

    val stepTime = 1f / config.updatesPerSecond

    // fraction of tangential slip at a contact converted into spin (0 = frictionless, 1 = full grip)
    protected open val spinFriction: Float = 0.8f

    /**
     * Transfer spin between two bodies from tangential friction at their contact point.
     *
     * For frictionless circles the contact impulse passes through both centres and imparts no
     * torque, so spin only arises from the relative *sliding* velocity at the point of contact.
     * We compute that slip, then apply an opposing angular impulse to each body scaled by
     * [spinFriction], driving the slip toward zero. Linear velocities are left untouched
     * (a deliberate simplification: friction here feeds rotation only).
     *
     * @param normal unit vector from a's centre toward b's centre at the moment of contact
     */
    protected fun applySpinFromCollision(a: Body, b: Body, normal: Vector2f) {
        val tangent = Vector2f(-normal.y, normal.x)
        // surface velocity of each body at the contact point along the tangent
        val contactVelocityA = a.angularVelocity * a.radius
        val contactVelocityB = -b.angularVelocity * b.radius
        val slip = Vector2f(b.velocity).sub(a.velocity).dot(tangent) + contactVelocityA + contactVelocityB
        if (abs(slip) < SLIP_EPSILON) return

        // disc moment of inertia I = m r^2 / 2; angular inverse effective mass for a tangential impulse
        val inertiaA = 0.5f * a.mass * a.radius * a.radius
        val inertiaB = 0.5f * b.mass * b.radius * b.radius
        val invAngularMass = (a.radius * a.radius) / inertiaA + (b.radius * b.radius) / inertiaB

        val jt = -spinFriction * slip / invAngularMass

        a.angularVelocity += jt * a.radius / inertiaA
        b.angularVelocity += jt * b.radius / inertiaB
    }

    protected fun integrateRotation(body: Body) {
        body.angle = (body.angle + body.angularVelocity * stepTime) % TAU
        if (body.angle < 0f) body.angle += TAU
    }

    companion object {
        const val TAU = (2 * Math.PI).toFloat()
        private const val SLIP_EPSILON = 1e-6f
    }


    override fun snapshotBodyViews(): List<BodyView> = lock.withLock {
        bodies.map { b -> BodyView(b.id, b.position.x, b.position.y, b.radius, b.velocity.x, b.velocity.y) }
    }

    override fun reset() {
        lock.withLock {
            bodies.clear()
            collisions.clear()
            pendingAdds.clear()
            currentStep = 0
            idSeq.set(1)
        }
    }

    fun addBody(body: Body) {
        pendingAdds.add(body)
    }

    fun drainAdds() {
        var b = pendingAdds.poll()
        while (b != null) {
            bodies.add(b)
            b = pendingAdds.poll()
        }
    }

    override fun step() {
        lock.withLock {
            drainAdds()
            doStepLocked()
            drainAdds()
            if (currentStep++ > 255) currentStep = 0
        }
    }

    override fun addBodies(bodies: List<Body>) {
        // attempt to add the newBodies to the simulator, adjusting them to fit into empty spaces closest to their intended locations
        for (b in bodies) {
            val movedBody = moveBody(b)
            if (movedBody == null) {
                logger.warn("ERROR: could not fit body $b onto grid, skipping to next.")
            } else {
                addBody(movedBody)
            }
        }
    }

    private fun moveBody(b: Body): Body? {
        if (!isOverlappingAny(b)) return b

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
            if (!isOverlappingAny(mutB)) {
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

    private fun isOverlappingAny(candidate: Body): Boolean =
        withBodiesRead { bs ->
            for (other in bs) {
                if (isOverlapping(candidate, other)) {
                    return@withBodiesRead true
                }
            }
            false
        }

    private fun isOverlapping(a: Body, b: Body): Boolean {
        val distanceApart = calculateDistance(a, b)
        val sumOfRadii = (a.radius + b.radius)
        return distanceApart < sumOfRadii
    }
}

package simulator

import config.WorldConfig
import domain.Body
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.ktor.server.config.MapApplicationConfig
import org.joml.Vector2f
import java.lang.Math.PI
import kotlin.math.abs
import kotlin.math.sqrt

class RotationTest : StringSpec({

    val config = WorldConfig(
        MapApplicationConfig(
            "world.width" to "40",
            "world.height" to "24",
            "world.updatesPerSecond" to "10", // stepTime = 0.1s
            "world.shouldAutoStart" to "false",
            "world.initialSpeed" to "1.5",
            "world.heartbeatTimeoutMillis" to "10000",
            "world.locationPattern" to "grid",
            "world.enableWrapping" to "false",
            "world.loggingRequests" to "false",
            "world.tcp.host" to "0.0.0.0",
            "world.tcp.port" to "9002",
            "world.tcp.readTimeoutMillis" to "30000",
            "world.tcp.framed.port" to "9003",
        )
    )

    fun bodyAt(x: Float, y: Float, vx: Float, vy: Float, radius: Float, mass: Float = 1f) = Body(
        id = 0,
        position = Vector2f(x, y),
        velocity = Vector2f(vx, vy),
        mass = mass,
        radius = radius,
        shapeId = 1,
    )

    fun slip(a: Body, b: Body, tangent: Vector2f): Float {
        // mirrors BaseBodySimulator.applySpinFromCollision's slip measure:
        // relative tangential velocity plus both bodies' surface speeds at the contact
        return Vector2f(b.velocity).sub(a.velocity).dot(tangent) +
            a.angularVelocity * a.radius + b.angularVelocity * b.radius
    }

    "north is derived from angle and stays on the unit circle" {
        val b = bodyAt(20f, 12f, 0f, 0f, 1f)

        b.angle = 0f
        b.north().x shouldBe 0f
        b.north().y shouldBe 1f

        // quarter turn counter-clockwise: north points along -X
        b.angle = (PI / 2).toFloat()
        abs(b.north().x + 1f) shouldBeLessThan 1e-6f
        abs(b.north().y) shouldBeLessThan 1e-6f

        // arbitrary angle still unit length
        b.angle = 1.2345f
        val len = sqrt(b.north().x * b.north().x + b.north().y * b.north().y)
        abs(len - 1f) shouldBeLessThan 1e-6f
    }

    "rotation integrates by angular velocity times step time" {
        val sim = BoundedWorldSimulator(config)
        val b = bodyAt(20f, 12f, 0f, 0f, 2f)
        b.angularVelocity = (PI / 2).toFloat() // quarter turn per second
        sim.addBodies(listOf(b))
        (sim as BaseBodySimulator).drainAdds()

        sim.step()

        b.angle shouldBe ((PI / 2).toFloat() * 0.1f)
    }

    "head-on collision imparts no spin when there is no tangential slip" {
        val sim = BoundedWorldSimulator(config)
        val a = bodyAt(11f, 12f, 4f, 0f, 1f)
        val b = bodyAt(13f, 12f, -4f, 0f, 1f)

        sim.resolveCollision(a, b)

        a.angularVelocity shouldBe 0f
        b.angularVelocity shouldBe 0f
    }

    "grazing collision transfers spin from tangential slip and drives the slip toward zero" {
        val sim = BoundedWorldSimulator(config)
        // offset vertically so the contact has a tangential component: a slides past b
        val a = bodyAt(11.8f, 11.5f, 4f, 0f, 1f)
        val b = bodyAt(12.6f, 12.4f, 0f, 0f, 1f)

        val normal = Vector2f(b.position).sub(a.position).normalize()
        val tangent = Vector2f(-normal.y, normal.x)
        val slipBefore = slip(a, b, tangent)
        abs(slipBefore) shouldBeGreaterThan 0.5f

        sim.resolveCollision(a, b)

        val slipAfter = slip(a, b, tangent)

        abs(slipAfter) shouldBeLessThan abs(slipBefore)
        abs(a.angularVelocity) shouldBeGreaterThan 0f
        abs(b.angularVelocity) shouldBeGreaterThan 0f
    }

    "grazing collision conserves total spin energy exactly" {
        val sim = BoundedWorldSimulator(config)
        val a = bodyAt(11.8f, 11.5f, 4f, 0f, 1f, mass = 2f)
        val b = bodyAt(12.6f, 12.4f, 0f, 0f, 1f)
        // give them some pre-existing spin so the test exercises transfer, not just creation
        a.angularVelocity = 1.5f
        b.angularVelocity = -0.5f

        fun spinEnergy(): Float {
            fun inertia(body: Body) = 0.5f * body.mass * body.radius * body.radius
            return inertia(a) * a.angularVelocity * a.angularVelocity +
                inertia(b) * b.angularVelocity * b.angularVelocity
        }

        val energyBefore = spinEnergy()

        sim.resolveCollision(a, b)

        val energyAfter = spinEnergy()
        // exact conservation (float-exact by construction of the rescale); allow only rounding noise
        abs(energyAfter - energyBefore) shouldBeLessThan 1e-5f
    }

    "copy preserves rotation state" {
        val b = bodyAt(1f, 2f, 3f, 4f, 5f)
        b.angle = 2.5f
        b.angularVelocity = -0.75f

        val c = b.copy()

        c.angle shouldBe 2.5f
        c.angularVelocity shouldBe -0.75f
    }
})

package simulator

import config.WorldConfig
import domain.Body
import domain.Shape
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.ktor.server.config.MapApplicationConfig
import org.joml.Vector2f

class WorldSimulatorTest : StringSpec({
    val shapes = mutableListOf(
        Shape(0, 1f, 2, emptyList()),
        Shape(1, 2f, 2, emptyList()),
        Shape(2, 0.5f, 2, emptyList()),
    ).associateBy { it.id }

    val defaultWorldApplicationConfig = MapApplicationConfig(
        "world.width" to "20",
        "world.height" to "20",
        "world.scalingFactor" to "1",
        "world.updatesPerSecond" to "1",
        "world.shouldAutoStart" to "false",
        "world.initialSpeed" to "1.5",
        "world.heartbeatTimeoutMillis" to "10000",
        "world.locationPattern" to "grid",
        "world.enableWrapping" to "true",
        "world.tcp.host" to "0.0.0.0",
        "world.tcp.port" to "9002"
    )

    val defaultConfig = WorldConfig(defaultWorldApplicationConfig)

    "bodies on a collision course should collide and change velocities" {
        val bodyA = Body.from(position = Vector2f(2f, 2f), velocity = Vector2f(0.5f, 0f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(6f, 2f), velocity = Vector2f(-0.5f, 0f), shape = shapes[0]!!)
        val worldSimulator = WrappingWorldSimulator(defaultConfig).apply { addBodies(listOf(bodyA, bodyB)) }

        worldSimulator.step()
        bodyA.position.x shouldBe 2.5f
        bodyB.position.x shouldBe 5.5f

        worldSimulator.step()
        bodyA.position.x shouldBe 3.0f
        bodyB.position.x shouldBe 5.0f

        // 1 more step makes them collide, they are touching from the last step, so all movement will be backwards from where they came
        worldSimulator.step()
        bodyA.velocity.x shouldBe -0.5f
        bodyB.velocity.x shouldBe 0.5f
        bodyA.position.x shouldBe 2.5f
        bodyB.position.x shouldBe 5.5f
    }

    "bodies at non direct collision change direction according to angle of reflection" {
        // These are at perfect 45 degree angles to each other, and both start 2 steps away from perfect touching at start of step
        val bodyA = Body.from(position = Vector2f(3f, 3f), velocity = Vector2f(1f, 1f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(9f, 3f), velocity = Vector2f(-1f, 1f), shape = shapes[0]!!)
        val worldSimulator = WrappingWorldSimulator(defaultConfig).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        // leading up to collision
        worldSimulator.step()
        bodyA.position shouldBe Vector2f(4f, 4f)
        bodyA.velocity shouldBe Vector2f(1f, 1f)
        bodyB.position shouldBe Vector2f(8f, 4f)
        bodyB.velocity shouldBe Vector2f(-1f, 1f)

        // this moves them to touching, but right at the end of the step, so no change in velocity yet
        worldSimulator.step()
        bodyA.position shouldBe Vector2f(5f, 5f)
        bodyA.velocity shouldBe Vector2f(1f, 1f)
        bodyB.position shouldBe Vector2f(7f, 5f)
        bodyB.velocity shouldBe Vector2f(-1f, 1f)

        // now collide, they are touching, and this step moves them away from each other with perfect 90-degree angle change
        worldSimulator.step()
        bodyA.position shouldBe Vector2f(4f, 6f)
        bodyA.velocity shouldBe Vector2f(-1f, 1f)
        bodyB.position shouldBe Vector2f(8f, 6f)
        bodyB.velocity shouldBe Vector2f(1f, 1f)
    }

    "bodies at non direct collision change direction according to angle of reflection but different masses" {
        // These are at perfect 45 degree angles to each other, and both start 2 steps away from perfect touching at start of step
        val bodyA = Body.from(position = Vector2f(3f, 3f), velocity = Vector2f(1f, 1f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(9f, 3f), velocity = Vector2f(-1f, 1f), shape = shapes[1]!!)
        val worldSimulator = WrappingWorldSimulator(defaultConfig).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        // leading up to collision
        worldSimulator.step()
        bodyA.position shouldBe Vector2f(4f, 4f)
        bodyA.velocity shouldBe Vector2f(1f, 1f)
        bodyB.position shouldBe Vector2f(8f, 4f)
        bodyB.velocity shouldBe Vector2f(-1f, 1f)

        // this moves them to touching, but right at the end of the step, so no change in velocity yet
        worldSimulator.step()
        bodyA.position shouldBe Vector2f(5f, 5f)
        bodyA.velocity shouldBe Vector2f(1f, 1f)
        bodyB.position shouldBe Vector2f(7f, 5f)
        bodyB.velocity shouldBe Vector2f(-1f, 1f)

        // now collide, they are touching, and this step moves them away from each other relative to their masses
        // A has lower mass than B so its x velocity is pushed harder
        worldSimulator.step()
        bodyA.position.x.shouldBe(3.33333f plusOrMinus(0.00002f))
        bodyA.position.y shouldBe 6f
        bodyA.velocity.x.shouldBe(-1.66667f plusOrMinus(0.00002f))
        bodyA.velocity.y shouldBe 1f
        bodyB.position.x.shouldBe(7.33333f plusOrMinus(0.00002f))
        bodyB.position.y shouldBe 6f
        bodyB.velocity.x.shouldBe(0.33333f plusOrMinus(0.00002f))
        bodyB.velocity.y shouldBe 1f
    }

    "bodies with different mass colliding" {
        val bodyA = Body.from(position = Vector2f(5f, 5f), velocity = Vector2f(0.75f, 0f), shape = shapes[2]!!)
        val bodyB = Body.from(position = Vector2f(9f, 5f), velocity = Vector2f(-0.25f, 0f), shape = shapes[1]!!)
        val worldSimulator = WrappingWorldSimulator(defaultConfig).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        worldSimulator.step()
        bodyA.position.x shouldBe 5.75f
        bodyB.position.x shouldBe 8.75f

        worldSimulator.step()
        bodyA.position.x shouldBe 6.5f
        bodyB.position.x shouldBe 8.5f

        // 1 more step makes them collide, they are touching from the last step, so all movement will be backwards from where they came
        worldSimulator.step()
        bodyA.velocity.x shouldBe -0.85f
        bodyB.velocity.x shouldBe 0.15f
        bodyA.position.x shouldBe 5.65f
        bodyB.position.x shouldBe 8.65f
    }

    "bodies moving at high speed will still collide during the step" {
        val bodyA = Body.from(position = Vector2f(8f, 8f), velocity = Vector2f(5f, 0f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(11f, 8f), velocity = Vector2f(-5f, 0f), shape = shapes[0]!!)
        val worldSimulator = WrappingWorldSimulator(defaultConfig).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        // collision after 0.1s (of the 1s step), perfect reflection, so av -> -5,0, bv -> 5,0, so both move for 0.9s in new direction, i.e. 4.5 units in new dir
        // thus ap = 8.5 - 4.5 = (4,8), bp = 10.5 + 4.5 = (15,8)
        worldSimulator.step()
        bodyA.position shouldBe Vector2f(4f, 8f)
        bodyA.velocity shouldBe Vector2f(-5f, 0f)

        bodyB.position shouldBe Vector2f(15f, 8f)
        bodyB.velocity shouldBe Vector2f(5f, 0f)
    }

    "bodies not on a collision course should not change velocities" {
        // Setup WorldSimulator with two bodies not on a collision course
        val bodyA = Body.from(position = Vector2f(2f, 2f), velocity = Vector2f(1f, 0f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(7f, 7f), velocity = Vector2f(-1f, 0f), shape = shapes[0]!!)
        val worldSimulator = WrappingWorldSimulator(defaultConfig).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        worldSimulator.step()

        bodyA.velocity.x shouldBe 1f
        bodyB.velocity.x shouldBe -1f
    }

    "wrapping body that crosses to extreme right of world" {
        val config = WorldConfig(defaultWorldApplicationConfig).also { it.width = 10; it.height = 2 }
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(5.6f, 0f), shape = shapes[0]!!)
        val worldSimulator = WrappingWorldSimulator(config).apply { addBodies(mutableListOf(bodyA)) }
        worldSimulator.step()
        bodyA.position.x.shouldBe(5.6f plusOrMinus(0.01f))
        worldSimulator.step()
        bodyA.position.x.shouldBe(1.2f plusOrMinus(0.01f))
    }

    "wrapping body that crosses to extreme left of world" {
        val config = WorldConfig(defaultWorldApplicationConfig).also { it.width = 10; it.height = 2 }
        val bodyA = Body.from(position = Vector2f(3f, 0f), velocity = Vector2f(-5f, 0f), shape = shapes[0]!!)
        val worldSimulator = WrappingWorldSimulator(config).apply { addBodies(mutableListOf(bodyA)) }
        worldSimulator.step()
        bodyA.position.x.shouldBe(8f plusOrMinus(0.01f))
    }

    "wrapping body that crosses to extreme bottom of world" {
        val config = WorldConfig(defaultWorldApplicationConfig).also { it.width = 2; it.height = 10 }
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 5.6f), shape = shapes[0]!!)
        val worldSimulator = WrappingWorldSimulator(config).apply { addBodies(mutableListOf(bodyA)) }
        worldSimulator.step()
        bodyA.position.y.shouldBe(5.6f plusOrMinus(0.01f))
        worldSimulator.step()
        bodyA.position.y.shouldBe(1.2f plusOrMinus(0.01f))
    }

    "wrapping body that crosses to extreme top of world" {
        val config = WorldConfig(defaultWorldApplicationConfig).also { it.width = 2; it.height = 10 }
        val bodyA = Body.from(position = Vector2f(0f, 3f), velocity = Vector2f(0f, -5f), shape = shapes[0]!!)
        val worldSimulator = WrappingWorldSimulator(config).apply { addBodies(mutableListOf(bodyA)) }
        worldSimulator.step()
        bodyA.position.y.shouldBe(8f plusOrMinus(0.01f))
    }

    "scaled world simulator works as non scaled version but with larger values" {
        val config = WorldConfig(defaultWorldApplicationConfig).also { it.width = 80; it.height = 80; it.scalingFactor = 4 }
        // Positions and velocities work same, just the radius is larger than the shape indicates, so collisions happen sooner as the body is scaled up to world sizes
        val bodyA = Body.from(position = Vector2f(10f, 10f), velocity = Vector2f(2f, 0f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(26f, 10f), velocity = Vector2f(-2f, 0f), shape = shapes[0]!!)
        val worldSimulator = WrappingWorldSimulator(config).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        worldSimulator.step()
        bodyA.position.x shouldBe 12.0f
        bodyB.position.x shouldBe 24.0f

        worldSimulator.step()
        bodyA.position.x shouldBe 14.0f
        bodyB.position.x shouldBe 22.0f

        // 1 more step makes them collide, they are touching from the last step, so all movement will be backwards from where they came
        worldSimulator.step()
        bodyA.velocity.x shouldBe -2.0f
        bodyB.velocity.x shouldBe 2.0f
        bodyA.position.x shouldBe 12.0f
        bodyB.position.x shouldBe 24.0f
    }

    "cannot add to grid if it does not have enough space" {
        val config = WorldConfig(defaultWorldApplicationConfig).also { it.width = 5; it.height = 5 }
        val shapesForSpaceTest = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
            1 to Shape(1, 1.0f, 2, emptyList()),
            2 to Shape(2, 1.0f, 1, emptyList()),
        )
        val worldSimulator = WrappingWorldSimulator(config)
        // fill it with 1 body of width 5,5
        val bodyA = Body.from(position = Vector2f(2.5f, 2.5f), velocity = Vector2f(0f, 0f), shape = shapesForSpaceTest[0]!!)
        worldSimulator.addBodies(listOf(bodyA))

        // try to add a 2x2, it won't fit anywhere, so should have only 1 body in world
        val bodyB = Body.from(position = Vector2f(1.2f, 1.2f), velocity = Vector2f(0f, 0f), shape = shapesForSpaceTest[1]!!)
        worldSimulator.addBodies(listOf(bodyB))
        worldSimulator.bodies.size shouldBe 1

        // try to add a 1x1, it will fit after spiraling out eventually to 0,0, because of circles, we can just fit it into a corner
        val bodyC = Body.from(position = Vector2f(2.2f, 2.3f), velocity = Vector2f(0f, 0f), shape = shapesForSpaceTest[2]!!)
        worldSimulator.addBodies(listOf(bodyC))
        worldSimulator.bodies.size shouldBe 2
        worldSimulator.bodies[1].position.x.shouldBe(0.2f plusOrMinus(0.001f))
        worldSimulator.bodies[1].position.y.shouldBe(0.3f plusOrMinus(0.001f))
    }

    "can detect collision at boundary" {
        val config = WorldConfig(defaultWorldApplicationConfig).also { it.width = 160; it.height = 80; it.scalingFactor = 4 }
        val shapesForBoundaryTest = mutableMapOf(
            0 to Shape(2, 1.0f, 5, emptyList())
        )
        val worldSimulator = WrappingWorldSimulator(config)
        val bodyA = Body.from(position = Vector2f(31.375097f, 3.468848f), velocity = Vector2f(-0.54833025f, -0.12327973f), shape = shapesForBoundaryTest[0]!!)
        val bodyB = Body.from(position = Vector2f(12.075428f, 77.72633f), velocity = Vector2f(0.05472869f, 0.033775542f), shape = shapesForBoundaryTest[0]!!)
        worldSimulator.addBodies(listOf(bodyA, bodyB))

        // work out the distance to the nearest version of B in wrapped (toroidal) space
        var distance = worldSimulator.calculateDistance(bodyA, bodyB)
        distance.shouldBe(20.135881f plusOrMinus(0.000001f))

        // find the position of B that is closest to A in toroidal mapping
        val closestB = worldSimulator.findClosestWrappedPosition(bodyA.position, bodyB.position)
        closestB.x.shouldBe(12.075428f plusOrMinus(0.000001f))
        closestB.y.shouldBe(-2.273666f plusOrMinus(0.000001f))

        var collisionTimes = worldSimulator.calculateCollisionTimes(bodyA, bodyB)
        collisionTimes shouldHaveSize 1
        collisionTimes[0].shouldBe(0.21817695f plusOrMinus(0.000001f))

        // we know these should collide, as the time is between 0 and 1 (which is our step time)
        worldSimulator.step()

        // calculate new distance apart, should still be over 20 as we bounced
        distance = worldSimulator.calculateDistance(bodyA, bodyB)
        distance.shouldBe(20.486927f plusOrMinus(0.000001f))

        // check a and b are at correct locations and speeds (note directions have changed)
        bodyA.position.x.shouldBe(31.293434f plusOrMinus(0.000001f))
        bodyA.position.y.shouldBe(3.4845412f plusOrMinus(0.000001f))
        bodyA.velocity.x.shouldBe(0.048564315f plusOrMinus(0.000001f))
        bodyA.velocity.y.shouldBe(0.054475166f plusOrMinus(0.000001f))
        bodyB.position.x.shouldBe(11.663491f plusOrMinus(0.000001f))
        bodyB.position.y.shouldBe(77.62114f plusOrMinus(0.000001f))
        bodyB.velocity.x.shouldBe(-0.5421659f plusOrMinus(0.000001f))
        bodyB.velocity.y.shouldBe(-0.14397936f plusOrMinus(0.000001f))

        // and there's no longer a collision about to happen as they are moving apart
        collisionTimes = worldSimulator.calculateCollisionTimes(bodyA, bodyB)
        collisionTimes shouldHaveSize 0

    }

})
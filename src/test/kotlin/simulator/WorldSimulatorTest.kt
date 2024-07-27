package simulator

import domain.Body
import domain.GameClient
import domain.Shape
import domain.VisibleShape
import geometry.Point
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.joml.Vector2f
import org.junit.jupiter.api.Test

class WorldSimulatorTest {
    private val shapes = mutableListOf(
        Shape(0, 1f, 2, emptyList()),
        Shape(1, 2f, 2, emptyList()),
        Shape(2, 0.5f, 2, emptyList()),
    ).associateBy { it.id }

    @Test
    fun `bodies on a collision course should collide and change velocities`() {
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0.5f, 0f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(4f, 0f), velocity = Vector2f(-0.5f, 0f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(20, 20, 1).apply { addBodies(listOf(bodyA, bodyB)) }

        worldSimulator.step(false)
        assertThat(bodyA.position.x).isEqualTo(0.5f)
        assertThat(bodyB.position.x).isEqualTo(3.5f)

        worldSimulator.step(false)
        assertThat(bodyA.position.x).isEqualTo(1.0f)
        assertThat(bodyB.position.x).isEqualTo(3.0f)

        // 1 more step makes them collide, they are touching from the last step, so all movement will be backwards from where they came
        worldSimulator.step(false)
        assertThat(bodyA.velocity.x).isEqualTo(-0.5f)
        assertThat(bodyB.velocity.x).isEqualTo(0.5f)
        assertThat(bodyA.position.x).isEqualTo(0.5f)
        assertThat(bodyB.position.x).isEqualTo(3.5f)
    }

    @Test
    fun `bodies at non direct collision change direction according to angle of reflection`() {
        // These are at perfect 45 degree angles to each other, and both start 2 steps away from perfect touching at start of step
        val bodyA = Body.from(position = Vector2f(-2f, -2f), velocity = Vector2f(1f, 1f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(4f, -2f), velocity = Vector2f(-1f, 1f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(20, 20, 1).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        // leading up to collision
        worldSimulator.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(-1f, -1f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(1f, 1f))
        assertThat(bodyB.position).isEqualTo(Vector2f(3f, -1f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(-1f, 1f))

        // this moves them to touching, but right at the end of the step, so no change in velocity yet
        worldSimulator.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(0f, 0f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(1f, 1f))
        assertThat(bodyB.position).isEqualTo(Vector2f(2f, 0f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(-1f, 1f))

        // now collide, they are touching, and this step moves them away from each other with perfect 90-degree angle change
        worldSimulator.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(-1f, 1f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(-1f, 1f))
        assertThat(bodyB.position).isEqualTo(Vector2f(3f, 1f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(1f, 1f))
    }

    @Test
    fun `bodies at non direct collision change direction according to angle of reflection but different masses`() {
        // These are at perfect 45 degree angles to each other, and both start 2 steps away from perfect touching at start of step
        val bodyA = Body.from(position = Vector2f(-2f, -2f), velocity = Vector2f(1f, 1f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(4f, -2f), velocity = Vector2f(-1f, 1f), shape = shapes[1]!!)
        val worldSimulator = WorldSimulator(20, 20, 1).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        // leading up to collision
        worldSimulator.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(-1f, -1f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(1f, 1f))
        assertThat(bodyB.position).isEqualTo(Vector2f(3f, -1f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(-1f, 1f))

        // this moves them to touching, but right at the end of the step, so no change in velocity yet
        worldSimulator.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(0f, 0f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(1f, 1f))
        assertThat(bodyB.position).isEqualTo(Vector2f(2f, 0f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(-1f, 1f))

        // now collide, they are touching, and this step moves them away from each other relative to their masses
        // A has lower mass than B so its x velocity is pushed harder
        worldSimulator.step(false)
        assertThat(bodyA.position.x).isCloseTo(-1.66667f, Offset.offset(0.00002f))
        assertThat(bodyA.position.y).isEqualTo(1f)
        assertThat(bodyA.velocity.x).isCloseTo(-1.66667f, Offset.offset(0.00002f))
        assertThat(bodyA.velocity.y).isEqualTo(1f)
        assertThat(bodyB.position.x).isCloseTo(2.33333f, Offset.offset(0.00002f))
        assertThat(bodyB.position.y).isEqualTo(1f)
        assertThat(bodyB.velocity.x).isCloseTo(0.33333f, Offset.offset(0.00002f))
        assertThat(bodyB.velocity.y).isEqualTo(1f)
    }

    @Test
    fun `bodies with different mass colliding`() {
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0.75f, 0f), shape = shapes[2]!!)
        val bodyB = Body.from(position = Vector2f(4f, 0f), velocity = Vector2f(-0.25f, 0f), shape = shapes[1]!!)
        val worldSimulator = WorldSimulator(20, 20, 1).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        worldSimulator.step(false)
        assertThat(bodyA.position.x).isEqualTo(0.75f)
        assertThat(bodyB.position.x).isEqualTo(3.75f)

        worldSimulator.step(false)
        assertThat(bodyA.position.x).isEqualTo(1.5f)
        assertThat(bodyB.position.x).isEqualTo(3.5f)

        // 1 more step makes them collide, they are touching from the last step, so all movement will be backwards from where they came
        worldSimulator.step(false)
        assertThat(bodyA.velocity.x).isEqualTo(-0.85f)
        assertThat(bodyB.velocity.x).isEqualTo(0.15f)
        assertThat(bodyA.position.x).isEqualTo(0.65f)
        assertThat(bodyB.position.x).isEqualTo(3.65f)
    }

    @Test
    fun `bodies moving at high speed will still collide during the step`() {
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(5f, 0f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(3f, 0f), velocity = Vector2f(-5f, 0f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(20, 20, 1).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        // collision after 0.1s (of the 1s step), perfect reflection, so av -> -5,0, bv -> 5,0, so both move for 0.9s in new direction, i.e. 4.5 units in new dir
        // thus ap = 0.5 - 4.5 = (-4,0), bp = 2.5 + 4.5 = (7,0)
        worldSimulator.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(-4f, 0f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(-5f, 0f))

        assertThat(bodyB.position).isEqualTo(Vector2f(7f, 0f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(5f, 0f))
    }

    @Test
    fun `bodies not on a collision course should not change velocities`() {
        // Setup WorldSimulator with two bodies not on a collision course
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(1f, 0f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(5f, 5f), velocity = Vector2f(-1f, 0f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(10, 10, 1).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        worldSimulator.step(false)

        assertThat(bodyA.velocity.x).isEqualTo(1f)
        assertThat(bodyB.velocity.x).isEqualTo(-1f)
    }

    @Test
    fun `wrapping body that crosses to extreme right of world`() {
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(5.6f, 0f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(10, 2, 1).apply { addBodies(mutableListOf(bodyA)) }
        worldSimulator.step()
        assertThat(bodyA.position.x).isCloseTo(5.6f, Offset.offset(0.01f))
        worldSimulator.step()
        assertThat(bodyA.position.x).isCloseTo(1.2f, Offset.offset(0.01f))
    }

    @Test
    fun `wrapping body that crosses to extreme left of world`() {
        val bodyA = Body.from(position = Vector2f(3f, 0f), velocity = Vector2f(-5f, 0f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(10, 2, 1).apply { addBodies(mutableListOf(bodyA)) }
        worldSimulator.step()
        assertThat(bodyA.position.x).isCloseTo(8f, Offset.offset(0.01f))
    }

    @Test
    fun `wrapping body that crosses to extreme bottom of world`() {
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 5.6f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(2, 10, 1).apply { addBodies(mutableListOf(bodyA)) }
        worldSimulator.step()
        assertThat(bodyA.position.y).isCloseTo(5.6f, Offset.offset(0.01f))
        worldSimulator.step()
        assertThat(bodyA.position.y).isCloseTo(1.2f, Offset.offset(0.01f))
    }

    @Test
    fun `wrapping body that crosses to extreme top of world`() {
        val bodyA = Body.from(position = Vector2f(0f, 3f), velocity = Vector2f(0f, -5f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(2, 10, 1).apply { addBodies(mutableListOf(bodyA)) }
        worldSimulator.step()
        assertThat(bodyA.position.y).isCloseTo(8f, Offset.offset(0.01f))
    }

    @Test
    fun `scaled world simulator works as non scaled version but with larger values`() {
        // Positions and velocities work same, just the radius is larger than the shape indicates, so collisions happen sooner as the body is scaled up to world sizes
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(2f, 0f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(16f, 0f), velocity = Vector2f(-2f, 0f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(80, 80, 4).apply { addBodies(mutableListOf(bodyA, bodyB)) }

        worldSimulator.step(false)
        assertThat(bodyA.position.x).isEqualTo(2.0f)
        assertThat(bodyB.position.x).isEqualTo(14.0f)

        worldSimulator.step(false)
        assertThat(bodyA.position.x).isEqualTo(4.0f)
        assertThat(bodyB.position.x).isEqualTo(12.0f)

        // 1 more step makes them collide, they are touching from the last step, so all movement will be backwards from where they came
        worldSimulator.step(false)
        assertThat(bodyA.velocity.x).isEqualTo(-2.0f)
        assertThat(bodyB.velocity.x).isEqualTo(2.0f)
        assertThat(bodyA.position.x).isEqualTo(2.0f)
        assertThat(bodyB.position.x).isEqualTo(14.0f)
    }

    @Test
    fun `corners of 1x1 body is just the point itself four times`() {
        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 1, emptyList()),
        )

        // 1 x 1 shape
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(80, 80, 1).apply { addBodies(mutableListOf(bodyA)) }
        assertThat(worldSimulator.bodyCorners(bodyA)).containsExactlyInAnyOrder(Point(0,0), Point(0,0), Point(0,0), Point(0,0))
    }

    @Test
    fun `corners of 2x2 body do not wrap on the left as upper left corner contains centre point`() {
        val shapes = mutableMapOf(
            0 to Shape(1, 1.0f, 2, emptyList()),
        )

        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(80, 80, 1).apply { addBodies(mutableListOf(bodyA)) }
        assertThat(worldSimulator.bodyCorners(bodyA)).containsExactlyInAnyOrder(Point(0,0), Point(1,1), Point(1, 0), Point(0, 1))
    }

    @Test
    fun `corners of 2x2 body wrap on the right and down when in bottom right corner`() {
        val shapes = mutableMapOf(
            0 to Shape(1, 1.0f, 2, emptyList()),
        )

        val bodyA = Body.from(position = Vector2f(9f, 9f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(10, 10, 1).apply { addBodies(mutableListOf(bodyA)) }
        assertThat(worldSimulator.bodyCorners(bodyA)).containsExactlyInAnyOrder(Point(9,9), Point(0,9), Point(9, 0), Point(0, 0))
    }

    @Test
    fun `corners of 3x3 body wrap from 0,0 to 4 corners`() {
        val shapes = mutableMapOf(
            0 to Shape(2, 1.0f, 3, emptyList()),
        )

        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        val worldSimulator = WorldSimulator(10, 10, 1).apply { addBodies(mutableListOf(bodyA)) }
        assertThat(worldSimulator.bodyCorners(bodyA)).containsExactlyInAnyOrder(Point(9,9), Point(9, 1), Point(1, 9), Point(1,1))
    }

    @Test
    fun `cannot add to grid if it does not have enough space`() {
        val shapes = mutableMapOf(
            0 to Shape(2, 1.0f, 5, emptyList()),
            1 to Shape(2, 1.0f, 2, emptyList()),
            2 to Shape(2, 1.0f, 1, emptyList()),
        )
        val worldSimulator = WorldSimulator(5, 5, 1)
        // fill it with 1 body of width 5,5
        val bodyA = Body.from(position = Vector2f(2.5f, 2.5f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        worldSimulator.addBodies(listOf(bodyA))

        // try to add a 2x2, it won't fit anywhere, so should have only 1 body in world
        val bodyB = Body.from(position = Vector2f(1.2f, 1.2f), velocity = Vector2f(0f, 0f), shape = shapes[1]!!)
        worldSimulator.addBodies(listOf(bodyB))
        assertThat(worldSimulator.bodies.size).isEqualTo(1)

        // try to add a 1x1, it will fit after spiraling out eventually to 0,0, because of circles, we can just fit it into a corner
        val bodyC = Body.from(position = Vector2f(2.2f, 2.3f), velocity = Vector2f(0f, 0f), shape = shapes[2]!!)
        worldSimulator.addBodies(listOf(bodyC))
        assertThat(worldSimulator.bodies.size).isEqualTo(2)
        assertThat(worldSimulator.bodies[1].position.x).isCloseTo(0.2f, Offset.offset(0.001f))
        assertThat(worldSimulator.bodies[1].position.y).isCloseTo(0.3f, Offset.offset(0.001f))
    }

    @Test
    fun `can detect collision at boundary`() {
        val shapes = mutableMapOf(
            0 to Shape(2, 1.0f, 5, emptyList())
        )
        val worldSimulator = WorldSimulator(160, 80, 4)
        val bodyA = Body.from(position = Vector2f(31.375097f, 3.468848f), velocity = Vector2f(-0.54833025f, -0.12327973f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(12.075428f, 77.72633f), velocity = Vector2f(0.05472869f, 0.033775542f), shape = shapes[0]!!)
        worldSimulator.addBodies(listOf(bodyA, bodyB))

        // work out the distance to the nearest version of B in wrapped (toroidal) space
        var distance = worldSimulator.calculateDistance(bodyA, bodyB)
        assertThat(distance).isCloseTo(20.135881f, Offset.offset(0.000001f))

        // find the position of B that is closest to A in toroidal mapping
        val closestB = worldSimulator.findClosestWrappedPosition(bodyA.position, bodyB.position)
        assertThat(closestB.x).isCloseTo(12.075428f, Offset.offset(0.000001f))
        assertThat(closestB.y).isCloseTo(-2.273666f, Offset.offset(0.000001f))

        var collisionTimes = worldSimulator.calculateCollisionTimes(bodyA, bodyB)
        assertThat(collisionTimes).hasSize(1)
        assertThat(collisionTimes[0]).isCloseTo(0.21817695f, Offset.offset(0.000001f))

        // we know these should collide, as the time is between 0 and 1 (which is our step time)
        worldSimulator.step()

        // calculate new distance apart, should still be over 20 as we bounced
        distance = worldSimulator.calculateDistance(bodyA, bodyB)
        assertThat(distance).isCloseTo(20.486927f, Offset.offset(0.000001f))

        // check a and b are at correct locations and speeds (note directions have changed)
        assertThat(bodyA.position.x).isCloseTo(31.293434f, Offset.offset(0.000001f))
        assertThat(bodyA.position.y).isCloseTo(3.4845412f, Offset.offset(0.000001f))
        assertThat(bodyA.velocity.x).isCloseTo(0.048564315f, Offset.offset(0.000001f))
        assertThat(bodyA.velocity.y).isCloseTo(0.054475166f, Offset.offset(0.000001f))
        assertThat(bodyB.position.x).isCloseTo(11.663491f, Offset.offset(0.000001f))
        assertThat(bodyB.position.y).isCloseTo(77.62114f, Offset.offset(0.000001f))
        assertThat(bodyB.velocity.x).isCloseTo(-0.5421659f, Offset.offset(0.000001f))
        assertThat(bodyB.velocity.y).isCloseTo(-0.14397936f, Offset.offset(0.000001f))

        // and there's no longer a collision about to happen as they are moving apart
        collisionTimes = worldSimulator.calculateCollisionTimes(bodyA, bodyB)
        assertThat(collisionTimes).hasSize(0)

    }

    @Test
    fun `create visible shapes by client for 2 clients and single wrapping in horizontal direction`() {
        val clients = listOf(
            GameClient(id = "1", name = "gc1", position = Point(0, 0)),
            GameClient(id = "2", name = "gc2", position = Point(1, 0))
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val worldSimulator = WorldSimulator(100, 50, 1)
        val bodyA = Body.from(position = Vector2f(1f, 10f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        worldSimulator.addBodies(listOf(bodyA))

        val visibleShapesByClient = worldSimulator.findVisibleShapesByClient(clients)
        assertThat(visibleShapesByClient["1"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(1, 10)),      // body position without reflection
        )
        assertThat(visibleShapesByClient["2"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(101, 10)),     // body wrapped on the left in client 2
        )
    }

    @Test
    fun `create visible shapes by client for 2 clients and single wrapping with wrapping in vertical direction`() {
        val clients = listOf(
            GameClient(id = "1", name = "gc1", position = Point(0, 0)),
            GameClient(id = "2", name = "gc2", position = Point(0, 1))
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val worldSimulator = WorldSimulator(50, 100, 1)
        val bodyA = Body.from(position = Vector2f(10f, 1f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        worldSimulator.addBodies(listOf(bodyA))

        val visibleShapesByClient = worldSimulator.findVisibleShapesByClient(clients)
        assertThat(visibleShapesByClient["1"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(10, 1)),
        )
        assertThat(visibleShapesByClient["2"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(10, 101)),
        )
    }

    @Test
    fun `create visible shapes by client for 4 clients in all 4 directions from client 1 position`() {
        val clients = listOf(
            GameClient(id = "1", name = "gc1", position = Point(0, 0)),
            GameClient(id = "2", name = "gc2", position = Point(1, 0)),
            GameClient(id = "3", name = "gc3", position = Point(1, 1)),
            GameClient(id = "4", name = "gc4", position = Point(0, 1)),
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val worldSimulator = WorldSimulator(100, 100, 1)
        val bodyA = Body.from(position = Vector2f(1f, 1f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        worldSimulator.addBodies(listOf(bodyA))

        val visibleShapesByClient = worldSimulator.findVisibleShapesByClient(clients)
        assertThat(visibleShapesByClient["1"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(1, 1)),
        )
        assertThat(visibleShapesByClient["2"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(101, 1)),
        )
        assertThat(visibleShapesByClient["3"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(101, 101)),
        )
        assertThat(visibleShapesByClient["4"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(1, 101)),
        )
    }

    @Test
    fun `create visible shapes by client for 4 clients in all 4 directions from client 3 position`() {
        val clients = listOf(
            GameClient(id = "1", name = "gc1", position = Point(0, 0)),
            GameClient(id = "2", name = "gc2", position = Point(1, 0)),
            GameClient(id = "3", name = "gc3", position = Point(1, 1)),
            GameClient(id = "4", name = "gc4", position = Point(0, 1)),
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val worldSimulator = WorldSimulator(100, 100, 1)
        val bodyA = Body.from(position = Vector2f(51f, 51f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        worldSimulator.addBodies(listOf(bodyA))

        val visibleShapesByClient = worldSimulator.findVisibleShapesByClient(clients)
        assertThat(visibleShapesByClient["1"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(51, 51)),
        )
        assertThat(visibleShapesByClient["2"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(51, 51)),
        )
        assertThat(visibleShapesByClient["3"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(51, 51)),
        )
        assertThat(visibleShapesByClient["4"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(51, 51)),
        )
    }

    @Test
    fun `create visible shapes by client for 4 clients where only contained in client 2`() {
        val clients = listOf(
            GameClient(id = "1", name = "gc1", position = Point(0, 0)),
            GameClient(id = "2", name = "gc2", position = Point(1, 0)),
            GameClient(id = "3", name = "gc3", position = Point(1, 1)),
            GameClient(id = "4", name = "gc4", position = Point(0, 1)),
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val worldSimulator = WorldSimulator(100, 100, 1)
        val bodyA = Body.from(position = Vector2f(61f, 11f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        worldSimulator.addBodies(listOf(bodyA))

        val visibleShapesByClient = worldSimulator.findVisibleShapesByClient(clients)
        assertThat(visibleShapesByClient["1"]).isEmpty()
        assertThat(visibleShapesByClient["2"]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(61, 11)),
        )
        assertThat(visibleShapesByClient["3"]).isEmpty()
        assertThat(visibleShapesByClient["4"]).isEmpty()
    }

    @Test
    fun `create visible shapes by client for 4 clients with multiple bodies from previous tests`() {
        val clients = listOf(
            GameClient(id = "1", name = "gc1", position = Point(0, 0)),
            GameClient(id = "2", name = "gc2", position = Point(1, 0)),
            GameClient(id = "3", name = "gc3", position = Point(1, 1)),
            GameClient(id = "4", name = "gc4", position = Point(0, 1)),
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val worldSimulator = WorldSimulator(100, 100, 1)
        val bodyA = Body.from(position = Vector2f(1f, 1f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(51f, 51f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        val bodyC = Body.from(position = Vector2f(61f, 11f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        worldSimulator.addBodies(listOf(bodyA, bodyB, bodyC))

        val visibleShapesByClient = worldSimulator.findVisibleShapesByClient(clients)
        assertThat(visibleShapesByClient["1"]).containsExactlyInAnyOrder(
            VisibleShape(shapeId=0, position=Point(x=1, y=1)),
            VisibleShape(shapeId=0, position=Point(x=51, y=51)),
        )
        assertThat(visibleShapesByClient["2"]).containsExactlyInAnyOrder(
            VisibleShape(shapeId=0, position=Point(x=101, y=1)),
            VisibleShape(shapeId=0, position=Point(x=51, y=51)),
            VisibleShape(shapeId=0, position=Point(x=61, y=11)),
        )
        assertThat(visibleShapesByClient["3"]).containsExactlyInAnyOrder(
            VisibleShape(shapeId=0, position=Point(x=101, y=101)),
            VisibleShape(shapeId=0, position=Point(x=51, y=51)),
        )
        assertThat(visibleShapesByClient["4"]).containsExactlyInAnyOrder(
            VisibleShape(shapeId=0, position=Point(x=1, y=101)),
            VisibleShape(shapeId=0, position=Point(x=51, y=51)),
        )
    }

}
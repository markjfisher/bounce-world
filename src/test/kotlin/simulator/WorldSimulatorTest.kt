package simulator

import domain.Body
import domain.Shape
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
        val bodyA = Body(position = Vector2f(0f, 0f), velocity = Vector2f(0.5f, 0f), shapeId = 0)
        val bodyB = Body(position = Vector2f(4f, 0f), velocity = Vector2f(-0.5f, 0f), shapeId = 0)
        val world = WorldSimulator(20, 20, 1, mutableListOf(bodyA, bodyB), shapes)

        world.step(false)
        assertThat(bodyA.position.x).isEqualTo(0.5f)
        assertThat(bodyB.position.x).isEqualTo(3.5f)

        world.step(false)
        assertThat(bodyA.position.x).isEqualTo(1.0f)
        assertThat(bodyB.position.x).isEqualTo(3.0f)

        // 1 more step makes them collide, they are touching from the last step, so all movement will be backwards from where they came
        world.step(false)
        assertThat(bodyA.velocity.x).isEqualTo(-0.5f)
        assertThat(bodyB.velocity.x).isEqualTo(0.5f)
        assertThat(bodyA.position.x).isEqualTo(0.5f)
        assertThat(bodyB.position.x).isEqualTo(3.5f)
    }

    @Test
    fun `bodies at non direct collision change direction according to angle of reflection`() {
        // These are at perfect 45 degree angles to each other, and both start 2 steps away from perfect touching at start of step
        val bodyA = Body(position = Vector2f(-2f, -2f), velocity = Vector2f(1f, 1f), shapeId = 0)
        val bodyB = Body(position = Vector2f(4f, -2f), velocity = Vector2f(-1f, 1f), shapeId = 0)
        val world = WorldSimulator(20, 20, 1, mutableListOf(bodyA, bodyB), shapes)

        // leading up to collision
        world.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(-1f, -1f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(1f, 1f))
        assertThat(bodyB.position).isEqualTo(Vector2f(3f, -1f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(-1f, 1f))

        // this moves them to touching, but right at the end of the step, so no change in velocity yet
        world.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(0f, 0f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(1f, 1f))
        assertThat(bodyB.position).isEqualTo(Vector2f(2f, 0f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(-1f, 1f))

        // now collide, they are touching, and this step moves them away from each other with perfect 90-degree angle change
        world.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(-1f, 1f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(-1f, 1f))
        assertThat(bodyB.position).isEqualTo(Vector2f(3f, 1f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(1f, 1f))
    }

    @Test
    fun `bodies at non direct collision change direction according to angle of reflection but different masses`() {
        // These are at perfect 45 degree angles to each other, and both start 2 steps away from perfect touching at start of step
        val bodyA = Body(position = Vector2f(-2f, -2f), velocity = Vector2f(1f, 1f), shapeId = 0)
        val bodyB = Body(position = Vector2f(4f, -2f), velocity = Vector2f(-1f, 1f), shapeId = 1)
        val world = WorldSimulator(20, 20, 1, mutableListOf(bodyA, bodyB), shapes)

        // leading up to collision
        world.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(-1f, -1f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(1f, 1f))
        assertThat(bodyB.position).isEqualTo(Vector2f(3f, -1f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(-1f, 1f))

        // this moves them to touching, but right at the end of the step, so no change in velocity yet
        world.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(0f, 0f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(1f, 1f))
        assertThat(bodyB.position).isEqualTo(Vector2f(2f, 0f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(-1f, 1f))

        // now collide, they are touching, and this step moves them away from each other relative to their masses
        // A has lower mass than B so its x velocity is pushed harder
        world.step(false)
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
        val bodyA = Body(position = Vector2f(0f, 0f), velocity = Vector2f(0.75f, 0f), shapeId = 2)
        val bodyB = Body(position = Vector2f(4f, 0f), velocity = Vector2f(-0.25f, 0f), shapeId = 1)
        val world = WorldSimulator(20, 20, 1, mutableListOf(bodyA, bodyB), shapes)

        world.step(false)
        assertThat(bodyA.position.x).isEqualTo(0.75f)
        assertThat(bodyB.position.x).isEqualTo(3.75f)

        world.step(false)
        assertThat(bodyA.position.x).isEqualTo(1.5f)
        assertThat(bodyB.position.x).isEqualTo(3.5f)

        // 1 more step makes them collide, they are touching from the last step, so all movement will be backwards from where they came
        world.step(false)
        assertThat(bodyA.velocity.x).isEqualTo(-0.85f)
        assertThat(bodyB.velocity.x).isEqualTo(0.15f)
        assertThat(bodyA.position.x).isEqualTo(0.65f)
        assertThat(bodyB.position.x).isEqualTo(3.65f)
    }

    @Test
    fun `bodies moving at high speed will still collide during the step`() {
        val bodyA = Body(position = Vector2f(0f, 0f), velocity = Vector2f(5f, 0f), shapeId = 0)
        val bodyB = Body(position = Vector2f(3f, 0f), velocity = Vector2f(-5f, 0f), shapeId = 0)
        val world = WorldSimulator(20, 20, 1, mutableListOf(bodyA, bodyB), shapes)

        // collision after 0.1s (of the 1s step), perfect reflection, so av -> -5,0, bv -> 5,0, so both move for 0.9s in new direction, i.e. 4.5 units in new dir
        // thus ap = 0.5 - 4.5 = (-4,0), bp = 2.5 + 4.5 = (7,0)
        world.step(false)
        assertThat(bodyA.position).isEqualTo(Vector2f(-4f, 0f))
        assertThat(bodyA.velocity).isEqualTo(Vector2f(-5f, 0f))

        assertThat(bodyB.position).isEqualTo(Vector2f(7f, 0f))
        assertThat(bodyB.velocity).isEqualTo(Vector2f(5f, 0f))
    }

    @Test
    fun `bodies not on a collision course should not change velocities`() {
        // Setup WorldSimulator with two bodies not on a collision course
        val bodyA = Body(position = Vector2f(0f, 0f), velocity = Vector2f(1f, 0f), shapeId = 0)
        val bodyB = Body(position = Vector2f(5f, 5f), velocity = Vector2f(-1f, 0f), shapeId = 0)
        val world = WorldSimulator(10, 10, 1, mutableListOf(bodyA, bodyB), shapes)

        world.step(false)

        assertThat(bodyA.velocity.x).isEqualTo(1f)
        assertThat(bodyB.velocity.x).isEqualTo(-1f)
    }

    @Test
    fun `wrapping body that crosses to extreme right of world`() {
        val bodyA = Body(position = Vector2f(0f, 0f), velocity = Vector2f(5.6f, 0f), shapeId = 0)
        val world = WorldSimulator(10, 2, 1, mutableListOf(bodyA), shapes)
        world.step()
        assertThat(bodyA.position.x).isCloseTo(5.6f, Offset.offset(0.01f))
        world.step()
        assertThat(bodyA.position.x).isCloseTo(1.2f, Offset.offset(0.01f))
    }

    @Test
    fun `wrapping body that crosses to extreme left of world`() {
        val bodyA = Body(position = Vector2f(3f, 0f), velocity = Vector2f(-5f, 0f), shapeId = 0)
        val world = WorldSimulator(10, 2, 1, mutableListOf(bodyA), shapes)
        world.step()
        assertThat(bodyA.position.x).isCloseTo(8f, Offset.offset(0.01f))
    }

    @Test
    fun `wrapping body that crosses to extreme bottom of world`() {
        val bodyA = Body(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 5.6f), shapeId = 0)
        val world = WorldSimulator(2, 10, 1, mutableListOf(bodyA), shapes)
        world.step()
        assertThat(bodyA.position.y).isCloseTo(5.6f, Offset.offset(0.01f))
        world.step()
        assertThat(bodyA.position.y).isCloseTo(1.2f, Offset.offset(0.01f))
    }

    @Test
    fun `wrapping body that crosses to extreme top of world`() {
        val bodyA = Body(position = Vector2f(0f, 3f), velocity = Vector2f(0f, -5f), shapeId = 0)
        val world = WorldSimulator(2, 10, 1, mutableListOf(bodyA), shapes)
        world.step()
        assertThat(bodyA.position.y).isCloseTo(8f, Offset.offset(0.01f))
    }

    @Test
    fun `scaled world simulator works as non scaled version but with larger values`() {
        // Positions and velocities work same, just the radius is larger than the shape indicates, so collisions happen sooner as the body is scaled up to world sizes
        val bodyA = Body(position = Vector2f(0f, 0f), velocity = Vector2f(2f, 0f), shapeId = 0)
        val bodyB = Body(position = Vector2f(16f, 0f), velocity = Vector2f(-2f, 0f), shapeId = 0)
        val world = WorldSimulator(80, 80, 4, mutableListOf(bodyA, bodyB), shapes)

        world.step(false)
        assertThat(bodyA.position.x).isEqualTo(2.0f)
        assertThat(bodyB.position.x).isEqualTo(14.0f)

        world.step(false)
        assertThat(bodyA.position.x).isEqualTo(4.0f)
        assertThat(bodyB.position.x).isEqualTo(12.0f)

        // 1 more step makes them collide, they are touching from the last step, so all movement will be backwards from where they came
        world.step(false)
        assertThat(bodyA.velocity.x).isEqualTo(-2.0f)
        assertThat(bodyB.velocity.x).isEqualTo(2.0f)
        assertThat(bodyA.position.x).isEqualTo(2.0f)
        assertThat(bodyB.position.x).isEqualTo(14.0f)
    }

    @Test
    fun `can find points of bodies added to simulator`() {
        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 1, emptyList()),
            1 to Shape(1, 1.0f, 2, emptyList()),
            2 to Shape(2, 1.0f, 3, emptyList()),
        )

        // 1x1 body @5,5 should only cover 5,5
        val bodyA = Body(position = Vector2f(5f, 5f), velocity = Vector2f(0f, 0f), shapeId = 0)
        val world1 = WorldSimulator(80, 80, 1, mutableListOf(bodyA), shapes)
        assertThat(world1.gridPoints()).containsExactly(
            Point(5,5)
        )

        // 2x2 body @5,5 should cover 5,5 to 6,6 points
        val bodyB = Body(position = Vector2f(5f, 5f), velocity = Vector2f(0f, 0f), shapeId = 1)
        val world2 = WorldSimulator(80, 80, 1, mutableListOf(bodyB), shapes)
        assertThat(world2.gridPoints()).containsExactlyInAnyOrder(
            Point(5, 5),
            Point(6, 5),
            Point(5, 6),
            Point(6, 6)
        )

        // 3x3 body @5,5 should cover 4,4 to 6,6
        val bodyC = Body(position = Vector2f(5f, 5f), velocity = Vector2f(0f, 0f), shapeId = 2)
        val world3 = WorldSimulator(80, 80, 1, mutableListOf(bodyC), shapes)
        assertThat(world3.gridPoints()).containsExactlyInAnyOrder(
            Point(4, 4),
            Point(5, 4),
            Point(6, 4),
            Point(4, 5),
            Point(5, 5),
            Point(6, 5),
            Point(4, 6),
            Point(5, 6),
            Point(6, 6)
        )

        // a 2x2 and 3x3 body added to the world
        val bodyD = Body(position = Vector2f(5f, 5f), velocity = Vector2f(0f, 0f), shapeId = 1)
        val bodyE = Body(position = Vector2f(10f, 10f), velocity = Vector2f(0f, 0f), shapeId = 2)
        val world4 = WorldSimulator(80, 80, 1, mutableListOf(bodyD, bodyE), shapes)
        assertThat(world4.gridPoints()).containsExactlyInAnyOrder(
            Point(5, 5),
            Point(6, 5),
            Point(5, 6),
            Point(6, 6),
            Point(9, 9),
            Point(10, 9),
            Point(11, 9),
            Point(9, 10),
            Point(10, 10),
            Point(11, 10),
            Point(9, 11),
            Point(10, 11),
            Point(11, 11)
        )
    }

    @Test
    fun `can calculate points that wrap at edges`() {
        val shapes = mutableMapOf(
            0 to Shape(2, 1.0f, 3, emptyList()),
        )

        // 3x3 body @0,0 should wrap left and up
        val bodyA = Body(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 0f), shapeId = 0)
        val world1 = WorldSimulator(5, 5, 1, mutableListOf(bodyA), shapes)
        assertThat(world1.gridPoints()).containsExactlyInAnyOrder(
            Point(0, 0),
            Point(1, 0),
            Point(4, 0),
            Point(0, 1),
            Point(1, 1),
            Point(4, 1),
            Point(0, 4),
            Point(1, 4),
            Point(4, 4),
        )

        // 3x3 body @4,0 should wrap right and up
        val bodyB = Body(position = Vector2f(4f, 0f), velocity = Vector2f(0f, 0f), shapeId = 0)
        val world2 = WorldSimulator(5, 5, 1, mutableListOf(bodyB), shapes)
        assertThat(world2.gridPoints()).containsExactlyInAnyOrder(
            Point(3, 0),
            Point(4, 0),
            Point(0, 0),
            Point(3, 1),
            Point(4, 1),
            Point(0, 1),
            Point(3, 4),
            Point(4, 4),
            Point(0, 4),
        )

        // 3x3 body @0,4 should wrap left and down
        val bodyC = Body(position = Vector2f(0f, 4f), velocity = Vector2f(0f, 0f), shapeId = 0)
        val world3 = WorldSimulator(5, 5, 1, mutableListOf(bodyC), shapes)
        assertThat(world3.gridPoints()).containsExactlyInAnyOrder(
            Point(4, 3),
            Point(0, 3),
            Point(1, 3),
            Point(4, 4),
            Point(0, 4),
            Point(1, 4),
            Point(4, 0),
            Point(0, 0),
            Point(1, 0),
        )

        // 3x3 body @4,4 should wrap right and down
        val bodyD = Body(position = Vector2f(4f, 4f), velocity = Vector2f(0f, 0f), shapeId = 0)
        val world4 = WorldSimulator(5, 5, 1, mutableListOf(bodyD), shapes)
        assertThat(world4.gridPoints()).containsExactlyInAnyOrder(
            Point(3, 3),
            Point(4, 3),
            Point(0, 3),
            Point(3, 4),
            Point(4, 4),
            Point(0, 4),
            Point(3, 0),
            Point(4, 0),
            Point(0, 0),
        )
    }

    @Test
    fun `cannot add to grid if it does not have enough space`() {
        val shapes = mutableMapOf(
            0 to Shape(2, 1.0f, 5, emptyList()),
            1 to Shape(2, 1.0f, 2, emptyList()),
            2 to Shape(2, 1.0f, 1, emptyList()),
        )
        val world = WorldSimulator(5, 5, 1, mutableListOf(), shapes)
        // fill it with 1 body of width 5,5
        val bodyA = Body(position = Vector2f(2.5f, 2.5f), velocity = Vector2f(0f, 0f), shapeId = 0)
        world.addBodies(listOf(bodyA))

        // try to add a 2x2, it won't fit anywhere, so should have only 1 body in world
        val bodyB = Body(position = Vector2f(1.2f, 1.2f), velocity = Vector2f(0f, 0f), shapeId = 1)
        world.addBodies(listOf(bodyB))
        assertThat(world.bodies.size).isEqualTo(1)

        // try to add a 1x1, it will fit after spiraling out eventually to 0,0, because of circles, we can just fit it into a corner
        val bodyC = Body(position = Vector2f(2.2f, 2.3f), velocity = Vector2f(0f, 0f), shapeId = 2)
        world.addBodies(listOf(bodyC))
        assertThat(world.bodies.size).isEqualTo(2)
        assertThat(world.bodies[1].position.x).isCloseTo(0.2f, Offset.offset(0.001f))
        assertThat(world.bodies[1].position.y).isCloseTo(0.3f, Offset.offset(0.001f))
    }
}
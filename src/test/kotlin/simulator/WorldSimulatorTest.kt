package simulator

import domain.Body
import domain.Shape
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.joml.Vector2f
import org.junit.jupiter.api.Test

class WorldSimulatorTest {
    private val shapes = mutableListOf(
        Shape(0, 1f, 2, emptyList()),
        Shape(1, 2f, 2, emptyList()),
        Shape(2, 0.5f, 2, emptyList()),
    )
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
}
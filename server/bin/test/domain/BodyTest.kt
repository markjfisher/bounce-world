package domain

import geometry.Point
import org.assertj.core.api.Assertions.assertThat
import org.joml.Vector2f
import org.junit.jupiter.api.Test

class BodyTest {

    @Test
    fun `corners of 1x1 body is just the point itself four times`() {
        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 1, emptyList()),
        )

        // 1 x 1 shape
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        assertThat(bodyA.bodyCorners(1, 80, 80)).containsExactlyInAnyOrder(Point(0,0), Point(0,0), Point(0,0), Point(0,0))
    }

    @Test
    fun `corners of 2x2 body do not wrap on the left as upper left corner contains centre point`() {
        val shapes = mutableMapOf(
            0 to Shape(1, 1.0f, 2, emptyList()),
        )

        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        assertThat(bodyA.bodyCorners(1, 80, 80)).containsExactlyInAnyOrder(Point(0,0), Point(1,1), Point(1, 0), Point(0, 1))
    }

    @Test
    fun `corners of 2x2 body wrap on the right and down when in bottom right corner`() {
        val shapes = mutableMapOf(
            0 to Shape(1, 1.0f, 2, emptyList()),
        )

        val bodyA = Body.from(position = Vector2f(9f, 9f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        assertThat(bodyA.bodyCorners(1, 10, 10)).containsExactlyInAnyOrder(Point(9,9), Point(0,9), Point(9, 0), Point(0, 0))
    }

    @Test
    fun `corners of 3x3 body wrap from 0,0 to 4 corners`() {
        val shapes = mutableMapOf(
            0 to Shape(2, 1.0f, 3, emptyList()),
        )

        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        assertThat(bodyA.bodyCorners(1, 10, 10)).containsExactlyInAnyOrder(Point(9,9), Point(9, 1), Point(1, 9), Point(1,1))
    }

}
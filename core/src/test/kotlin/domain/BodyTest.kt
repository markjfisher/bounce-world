package domain

import geometry.Point
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.joml.Vector2f

data class BWShape(
    override val id: Int,
    override val mass: Float,
    val sideLength: Int,
    val data: List<Int>
) : Shape {
    override val radius: Float
        get() = sideLength / 2f

    override fun codedString(): String {
        return data.map { it.toChar() }.joinToString("")
    }
}

class BodyTest: StringSpec ({

    "corners of 1x1 body is just the point itself four times" {
        val shapes = mutableMapOf(
            0 to BWShape(0, 1.0f, 1, emptyList()),
        )

        // 1 x 1 shape
        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        bodyA.bodyCorners(80, 80).shouldContainExactlyInAnyOrder(Point(0,0), Point(0,0), Point(0,0), Point(0,0))
    }

    "corners of 2x2 body do not wrap on the left as upper left corner contains centre point" {
        val shapes = mutableMapOf(
            0 to BWShape(1, 1.0f, 2, emptyList()),
        )

        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        bodyA.bodyCorners(80, 80).shouldContainExactlyInAnyOrder(Point(0,0), Point(1,1), Point(1, 0), Point(0, 1))
    }

    "corners of 2x2 body wrap on the right and down when in bottom right corner" {
        val shapes = mutableMapOf(
            0 to BWShape(1, 1.0f, 2, emptyList()),
        )

        val bodyA = Body.from(position = Vector2f(9f, 9f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        bodyA.bodyCorners(10, 10).shouldContainExactlyInAnyOrder(Point(9,9), Point(0,9), Point(9, 0), Point(0, 0))
    }

    "corners of 3x3 body wrap from 0,0 to 4 corners" {
        val shapes = mutableMapOf(
            0 to BWShape(2, 1.0f, 3, emptyList()),
        )

        val bodyA = Body.from(position = Vector2f(0f, 0f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        bodyA.bodyCorners(10, 10).shouldContainExactlyInAnyOrder(Point(9,9), Point(9, 1), Point(1, 9), Point(1,1))
    }

})
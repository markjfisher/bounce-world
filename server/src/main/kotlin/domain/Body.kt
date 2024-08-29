package domain

import geometry.Point
import org.joml.Vector2f
import kotlin.math.roundToInt

data class Body(
    val id: Int = 0,
    val position: Vector2f,
    val velocity: Vector2f,
    val mass: Float,
    // although a Float, it's actually half of an integer from the sideLength, so will always double to a whole number.
    val radius: Float,
    val shapeId: Int,
) {
    val intendedPosition = Vector2f(position)

    fun copy(): Body {
        return Body(id, Vector2f(position), Vector2f(velocity), mass, radius, shapeId)
    }

    companion object {
        fun from(id: Int = 0, position: Vector2f, velocity: Vector2f, shape: Shape): Body {
            return Body(
                id = id,
                position = position,
                velocity = velocity,
                mass = shape.mass,
                radius = shape.sideLength / 2f,
                shapeId = shape.id
            )
        }
    }

    fun bodyCorners(scalingFactor: Int, width: Int, height: Int): List<Point> {
        val centre = Point(position.x.roundToInt(), position.y.roundToInt())
        val n = (radius * 2).roundToInt()
        // calculate the offsets to the centre point for grid positions this body covers
        val offsets = when {
            // even width needs offset of -[(n/2 -1),(n/2 -1)], +[n/2, n/2]
            n.mod(2) == 0 -> Pair(
                Point(n / 2 - 1, n / 2 - 1),
                Point(n / 2, n / 2)
            )
            // odd width needs offset of +/- [(n-1)/2]
            else -> Pair(
                Point((n - 1) / 2, (n - 1) / 2),
                Point((n - 1) / 2, (n - 1) / 2)
            )
        }
        // find the extreme points from centre with these offsets
        val topLeft = centre - offsets.first * scalingFactor
        val bottomRight = centre + offsets.second * scalingFactor
        val topRight = Point(bottomRight.x, topLeft.y)
        val bottomLeft = Point(topLeft.x, bottomRight.y)

        return listOf(topLeft, topRight, bottomLeft, bottomRight).map { p -> boundPoint(p, width, height) }
    }

    private fun boundPoint(p: Point, width: Int, height: Int): Point {
        val wrappedX = if (p.x < 0) {
            (p.x % width + width) % width
        } else {
            p.x % width
        }
        val wrappedY = if (p.y < 0) {
            (p.y % height + height) % height
        } else {
            p.y % height
        }
        return Point(wrappedX, wrappedY)
    }

}

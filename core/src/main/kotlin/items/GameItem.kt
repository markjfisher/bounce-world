package items

import geometry.Point
import org.joml.Vector2f
import kotlin.math.roundToInt

interface GameItem {
    val id: Int
    val position: Vector2f
    val velocity: Vector2f
    val mass: Float
    val radius: Float

    // angle of direction in radians. 0 = East, North = pi/2 (as all good maths should be)
    // This is needed because the item can be stationary, but potentially could move
    var direction: Double

    fun itemCorners(width: Int, height: Int, doBounding: Boolean = true): List<Point> {
        val centre = Point(position.x.roundToInt(), position.y.roundToInt())
        val n = (radius * 2).roundToInt()
        // calculate the offsets to the centre point for grid positions this body covers
        val offsets = when {
            // even width needs offset of -[(n/2 - 1), (n/2 - 1)], + [n/2, n/2]
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
        val topLeft = centre - offsets.first
        val bottomRight = centre + offsets.second
        val topRight = Point(bottomRight.x, topLeft.y)
        val bottomLeft = Point(topLeft.x, bottomRight.y)

        return listOf(topLeft, topRight, bottomLeft, bottomRight).map { p -> if (doBounding) boundPoint(p, width, height) else p }
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


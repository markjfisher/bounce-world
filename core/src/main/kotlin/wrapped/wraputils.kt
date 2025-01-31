package wrapped

import org.joml.Vector2f

fun findClosestWrappedPosition(a: Vector2f, b: Vector2f, width: Int, height: Int): Vector2f {
    val w = width.toFloat()
    val h = height.toFloat()
    // Define the shifts for the 8 surrounding positions plus the original (0,0) position
    val shifts = listOf(
        Pair(0f, 0f), // Original
        Pair(-w, 0f), // W
        Pair(w, 0f), // E
        Pair(0f, -h), // N
        Pair(0f, h), // S
        Pair(-w, -h), // NW
        Pair(w, -h), // NE
        Pair(-w, h), // SW
        Pair(w, h) // SE
    )

    val (dx, dy) = shifts.minBy { (dx, dy) ->
        val wrappedB = Vector2f(b.x + dx, b.y + dy)
        a.distance(wrappedB)
    }

    return Vector2f(b.x + dx, b.y + dy)
}
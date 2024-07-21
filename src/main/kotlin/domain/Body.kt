package domain

import org.joml.Vector2f

data class Body(
    // val id: Int = 0,
    val position: Vector2f,
    val velocity: Vector2f,
    val shapeId: Int,
) {
    val intendedPosition = Vector2f(position)
}

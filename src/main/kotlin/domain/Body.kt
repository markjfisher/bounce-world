package domain

import org.joml.Vector2f

data class Body(
    val position: Vector2f,
    val velocity: Vector2f,
    val shape: Shape
) {
    val intendedPosition = Vector2f(position)
}

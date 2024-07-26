package domain

import org.joml.Vector2f

data class Body(
    val id: Int = 0,
    val position: Vector2f,
    val velocity: Vector2f,
    val mass: Float,
    // although a Float, it's actually half of an integer from the sideLength, so will always double to an int.
    val radius: Float,
    val shapeId: Int,
) {
    val intendedPosition = Vector2f(position)

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
}

package domain

import org.joml.Vector2f

data class Body(
    val id: Int = 0,
    val position: Vector2f,
    val velocity: Vector2f,
    val mass: Float,
    // although a Float, it's actually half of an integer from the sideLength, so will always double to a whole number.
    val radius: Float,
    val shapeId: Int,
    // orientation of the body's local north, radians, measured counter-clockwise from world +Y
    var angle: Float = 0f,
    // spin rate, radians per second, positive is counter-clockwise
    var angularVelocity: Float = 0f,
) {
    val intendedPosition = Vector2f(position)

    // local north as a unit vector, derived from angle so it can never drift off the unit circle
    fun north(): Vector2f = Vector2f(
        -kotlin.math.sin(angle),
        kotlin.math.cos(angle),
    )

    fun copy(): Body {
        return Body(id, Vector2f(position), Vector2f(velocity), mass, radius, shapeId).also {
            it.angle = angle
            it.angularVelocity = angularVelocity
        }
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
}

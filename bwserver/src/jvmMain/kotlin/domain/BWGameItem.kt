package domain

import items.GameItem
import org.joml.Vector2f

data class BWGameItem(
    override val id: Int,
    override val position: Vector2f,
    override val velocity: Vector2f,
    override val mass: Float,
    override val radius: Float,
    val shapeId: Int,
): GameItem {
    companion object {
        fun from(id: Int = 0, position: Vector2f, velocity: Vector2f, shape: Shape): BWGameItem {
            return BWGameItem(
                id = id,
                position = position,
                velocity = velocity,
                mass = shape.mass,
                radius = shape.radius,
                shapeId = shape.id
            )
        }
    }
}

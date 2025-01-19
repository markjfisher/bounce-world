package items

import org.joml.Vector2f

data class Asteroid(
    override val id: Int,
    override val position: Vector2f,
    override val velocity: Vector2f,
    override val mass: Float,
    override val radius: Float,
    override var direction: Double
): GameItem {
}
package domain

import org.joml.Vector2f

data class VisibleShape(
    val shapeId: Int,
    // float so sub-world-unit precision survives until the final pixel scaling in asBinary
    val position: Vector2f,
    val bodyId: Int = 0
)

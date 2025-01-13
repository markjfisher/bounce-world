package domain

import geometry.Point

data class VisibleShape(
    val shapeId: Int,
    val position: Point,
    val bodyId: Int = 0
)

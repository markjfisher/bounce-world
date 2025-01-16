package domain

import geometry.Point

data class VisibleShape(
    val position: Point,
    val bodyId: Int = 0
)

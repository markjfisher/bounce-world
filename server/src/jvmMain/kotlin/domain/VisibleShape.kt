package domain

import org.joml.Vector2f

data class VisibleShape(
    val shapeId: Int,
    // float so sub-world-unit precision survives until the final pixel scaling in asBinary
    val position: Vector2f,
    val bodyId: Int = 0,
    // orientation of local north in radians [0, 2pi) and spin rate in rad/s; sent only to
    // clients that registered the rotation capability
    val angle: Float = 0f,
    val angularVelocity: Float = 0f,
)

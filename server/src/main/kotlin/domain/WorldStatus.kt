package domain

import geometry.Point
import kotlinx.serialization.Serializable

@Serializable
data class VectorData(
    val x: Float,
    val y: Float
)

@Serializable
data class BodyData(
    val id: Int,
    val radius: Float,
    val mass: Float,
    val position: VectorData,
    val velocity: VectorData
)

@Serializable
data class BodySummary(
    val size: Int,
    val count: Int
)

@Serializable
data class ClientData(
    val id: Int,
    val name: String,
    val location: Point
)

@Serializable
data class WorldStatus(
    val width: Int,
    val height: Int,
    val frozen: Boolean,
    val wrapping: Boolean,
    val bodyCounts: List<BodySummary>,
    val bodies: List<BodyData>,
    val clients: List<ClientData>
)

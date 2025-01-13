package bw

import kotlinx.serialization.Serializable

@Serializable
data class GameClientShared(
    val id: Int? = null,
    val name: String? = null,
    val version: Int? = null,
    val position: Pair<Int, Int>? = null,
    val screenSize: Pair<Int, Int>? = null
)

@Serializable
data class BodyShared(
    val id: Int? = null,
    val position: Pair<Float, Float>? = null,
    val velocity: Pair<Float, Float>? = null,
    val mass: Float? = null,
    val radius: Float? = null,
    val shapeId: Int? = null,
)

@Serializable
data class ShapeShared(
    val id: Int? = null,
    val mass: Float? = null,
    val sideLength: Int? = null,
    val data: List<Int>? = null,
)

@Serializable
data class WorldShared(
    val width: Int? = null,
    val height: Int? = null,
    val upTime: String? = null,
    val clients: Map<Int, GameClientShared>? = null,
    val isFrozen: Boolean? = null,
    val isWrapping: Boolean? = null,
    val bodies: List<BodyShared>? = null,
)

package domain

//import com.fasterxml.jackson.annotation.JsonInclude
import geometry.Point

data class VectorData(
    val x: Float,
    val y: Float
)

data class BodyData(
    val id: Int,
    val radius: Float,
    val mass: Float,
    val position: VectorData,
    val velocity: VectorData
)

data class BodySummary(
    val size: Int,
    val count: Int
)

data class ClientData(
    val id: Int,
    val name: String,
    val location: Point
)

data class WorldStatus(
    val width: Int,
    val height: Int,
    val frozen: Boolean,
    val wrapping: Boolean,
//    @JsonInclude(value= JsonInclude.Include.ALWAYS, content= JsonInclude.Include.ALWAYS)
    val bodyCounts: List<BodySummary>,
//    @JsonInclude(value= JsonInclude.Include.ALWAYS, content= JsonInclude.Include.ALWAYS)
    val bodies: List<BodyData>,
//    @JsonInclude(value= JsonInclude.Include.ALWAYS, content= JsonInclude.Include.ALWAYS)
    val clients: List<ClientData>
)

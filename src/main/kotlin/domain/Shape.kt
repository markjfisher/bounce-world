package domain

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.serde.annotation.Serdeable

data class Shape(
    val id: Int,
    val mass: Float,
    val sideLength: Int,
    val data: List<Int>,
) {
    fun codedString(): String {
        return data.map { it.toChar() }.joinToString("")
    }
}

// A convenience type that holds each shape in a single string of format "id,w,data" so the poor clients don't have to do many calls to get each value
@Serdeable
data class ShapeInfo(
    @JsonProperty(value = "d")
    val shapeData: String
)
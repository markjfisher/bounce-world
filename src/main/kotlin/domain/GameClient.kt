package domain

import geometry.Point
import io.micronaut.serde.annotation.Serdeable

@Serdeable.Deserializable
@Serdeable.Serializable
data class ScreenSize(var width: Int, var height: Int)

@Serdeable.Deserializable
@Serdeable.Serializable
data class GameClient(
    val id: String,
    val name: String,
    var position: Point = Point(0, 0),
    var screenSize: ScreenSize = ScreenSize(0, 0)
)

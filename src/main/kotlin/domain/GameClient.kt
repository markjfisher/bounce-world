package domain

import com.fasterxml.jackson.annotation.JsonIgnore
import domain.World.Companion.SCREEN_HEIGHT
import domain.World.Companion.SCREEN_WIDTH
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
    val version: Int = 1,
    var position: Point = Point(0, 0),
    var screenSize: ScreenSize = ScreenSize(0, 0),
    val viewWidth: Int = SCREEN_WIDTH,
    val viewHeight: Int = SCREEN_HEIGHT,
) {
    @JsonIgnore
    val worldBounds: Pair<Point, Point> = Pair(
        Point(position.x * viewWidth, position.y * viewHeight),
        Point((position.x + 1) * viewWidth - 1, (position.y + 1) * viewHeight)
    )
}

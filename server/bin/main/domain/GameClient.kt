package domain

import geometry.Point

data class ScreenSize(var width: Int, var height: Int)

data class GameClient(
    val id: Int,
    val name: String,
    val version: Int = 1,
    var position: Point = Point(0, 0),
    var screenSize: ScreenSize = ScreenSize(0, 0)
) {
    var worldBounds: Pair<Point, Point> = Pair(Point(0, 0), Point(0, 0))

    fun updateWorldBounds(width: Int, height: Int) {
        worldBounds = Pair(
            Point(position.x * width, position.y * height),
            Point((position.x + 1) * width - 1, (position.y + 1) * height - 1)
        )
    }

}

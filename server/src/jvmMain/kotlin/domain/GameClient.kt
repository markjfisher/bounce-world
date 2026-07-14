package domain

import geometry.Point

data class ScreenSize(var width: Int, var height: Int)

data class ClientRegion(
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
) {
    val upperLeft: Point
        get() = Point(x, y)

    val lowerRight: Point
        get() = Point(x + width - 1, y + height - 1)

    fun contains(point: Point): Boolean {
        return point.x in x until (x + width) && point.y in y until (y + height)
    }
}

data class GameClient(
    val id: Int,
    val name: String,
    val version: Int = 1,
    var position: Point = Point(0, 0),
    var screenSize: ScreenSize = ScreenSize(0, 0),
    var worldSize: ScreenSize = ScreenSize(0, 0),
) {
    var worldBounds: Pair<Point, Point> = Pair(Point(0, 0), Point(0, 0))
    var region: ClientRegion = ClientRegion()

    fun updateWorldBounds(width: Int, height: Int) {
        setRegion(position.x * width, position.y * height, width, height)
    }

    fun setRegion(x: Int, y: Int, width: Int, height: Int) {
        region = ClientRegion(x, y, width, height)
        worldBounds = Pair(region.upperLeft, region.lowerRight)
    }

}

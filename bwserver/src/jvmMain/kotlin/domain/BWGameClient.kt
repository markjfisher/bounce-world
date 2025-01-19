package domain

import geometry.Point

data class BWGameClient(
    override val id: Int,
    override val name: String,
    override val version: Int,
    override val screenWidth: Int,
    override val screenHeight: Int,
    var position: Point = Point(0, 0),
) : GameClientNew {
    var worldBounds: Pair<Point, Point> = Pair(Point(0, 0), Point(0, 0))

    fun updateWorldBounds(width: Int, height: Int) {
        worldBounds = Pair(
            Point(position.x * width, position.y * height),
            Point((position.x + 1) * width - 1, (position.y + 1) * height - 1)
        )
    }
}
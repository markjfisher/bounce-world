package geometry

enum class Direction {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    fun cw() = when(this) {
        NORTH -> EAST
        EAST -> SOUTH
        SOUTH -> WEST
        WEST -> NORTH
    }

    fun ccw() = when(this) {
        NORTH -> WEST
        WEST -> SOUTH
        SOUTH -> EAST
        EAST -> NORTH
    }

    fun opposite() = when(this) {
        NORTH -> SOUTH
        SOUTH -> NORTH
        WEST -> EAST
        EAST -> WEST
    }

    fun turnR(degrees: Int) = when (degrees) {
        90 -> this.cw()
        180 -> this.cw().cw()
        270 -> this.ccw()
        else -> throw Exception("Unknown angle: $degrees")
    }

    fun turnL(degrees: Int) = turnR(360 - degrees)

    fun toPoint(): Point {
        // strictly negative North, positive South, same for E/W
        return when(this) {
            NORTH -> Point(0,-1)
            EAST -> Point(1, 0)
            WEST -> Point(-1, 0)
            SOUTH -> Point(0, 1)
        }
    }

    companion object {
        fun from(s: String) : Direction = from(s.first())
        fun from(char: Char) : Direction = when(char.uppercaseChar()) {
            'N' -> NORTH
            'S' -> SOUTH
            'E' -> EAST
            'W' -> WEST

            'R' -> EAST
            'L' -> WEST
            'U' -> NORTH
            'D' -> SOUTH
            else -> throw IllegalArgumentException("Can't map $char to Direction")
        }

        fun from(c: Char, heading: Direction): Direction {
            val dir = c.uppercaseChar()
            return if (dir == 'F') return heading else from(dir)
        }

    }
}
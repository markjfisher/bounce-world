package geometry

class SpiralGenerator: LocationGenerator {
    private var direction = Direction.EAST
    private var currentPoint = Point(0, 0)
    private var steps = 1
    private var stepCounter = 0
    private var directionChangeCounter = 0

    override fun generate(): Sequence<Point> = sequence {
        yield(currentPoint) // Yield the initial point

        while (true) {
            if (stepCounter < steps) {
                currentPoint += direction
                stepCounter++
            } else {
                direction = direction.cw() // Change direction clockwise
                stepCounter = 0
                directionChangeCounter++
                if (directionChangeCounter % 2 == 0) {
                    steps++ // Increase the steps every 2 direction changes
                }
                continue
            }
            yield(currentPoint)
        }
    }
}
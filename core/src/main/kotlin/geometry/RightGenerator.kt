package geometry

class RightGenerator: LocationGenerator {
    private var currentPoint = Point(0, 0)

    override fun generate(): Sequence<Point> = sequence {
        yield(currentPoint) // Yield the initial point

        while (true) {
            currentPoint += Direction.EAST
            yield(currentPoint)
        }
    }
}
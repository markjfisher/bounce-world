package geometry

class GridPatternGenerator {
    private var currentPoint = Point(0, 0)
    private var direction = Direction.SOUTH
    private var currentWidth = 2
    private var haveFilled = 0


    fun generate(): Sequence<Point> = sequence {
        yield(currentPoint) // Yield the first point

        while (true) {
            if (haveFilled == (2 * currentWidth - 1)) {
                // all slots in current width sized box are done
                currentWidth++
                haveFilled = 0
            }
            // we get exactly currentWidth going down the side, and currentWidth - 1 to the left
            direction = if (haveFilled < currentWidth) Direction.SOUTH else Direction.WEST
            if (haveFilled == 0) {
                // calculate the first point of a new slice by noticing we can take the last point of previous slice, and swap X,Y to give its diagonally opposite point, which is 1 place to the left of the new point we need.
                currentPoint = Point(currentPoint.y + 1, currentPoint.x)
            } else {
                currentPoint += direction
            }
            haveFilled++
            yield(currentPoint)
        }
    }
}
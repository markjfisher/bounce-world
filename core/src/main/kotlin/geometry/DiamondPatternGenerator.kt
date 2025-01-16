package geometry

class DiamondPatternGenerator: LocationGenerator {
    private var currentPoint = Point(0, 0)
    private var directions = listOf(
        Point(-1, 1),  // SW
        Point(-1, -1), // NW
        Point(1, -1),  // NE
        Point(1, 1),   // SE
    )
    private var layer = 1
    private var numInLayer = 0
    override fun generate(): Sequence<Point> = sequence {
        // deal with first 2 points manually
        yield(currentPoint)
        currentPoint = Point(1, 0)

        // spiral out in a diamond shape like:
        //    5
        //  4 1 2
        //    3
        while(true) {
            yield(currentPoint)
            var nextPoint = currentPoint + directions[numInLayer / layer]
            numInLayer++
            if (numInLayer == layer * 4) {
                nextPoint += Direction.EAST
                numInLayer = 0
                layer++
            }
            currentPoint = nextPoint
        }
    }

}
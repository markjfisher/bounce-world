package geometry

interface LocationGenerator {
    fun generate(): Sequence<Point>
}

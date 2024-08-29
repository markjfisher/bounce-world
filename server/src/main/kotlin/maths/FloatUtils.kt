package maths

fun Pair<Float, Float>.equalsWithTolerance(other: Pair<Float, Float>, tolerance: Float = 0.0001f): Boolean {
    return this.first.isApproximatelyEqual(other.first, tolerance) && this.second.isApproximatelyEqual(other.second, tolerance)
}

fun Float.isApproximatelyEqual(other: Float, tolerance: Float): Boolean {
    return kotlin.math.abs(this - other) <= tolerance
}

fun List<Pair<Float, Float>>.distinctByTolerance(tolerance: Float = 0.0001f): List<Pair<Float, Float>> {
    val result = mutableListOf<Pair<Float, Float>>()
    this.forEach { current ->
        if (result.none { it.equalsWithTolerance(current, tolerance) }) {
            result.add(current)
        }
    }
    return result
}
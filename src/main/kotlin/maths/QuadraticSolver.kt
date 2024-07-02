package maths

import kotlin.math.sqrt

data class QuadraticSolver(
    val a: Float,
    val b: Float,
    val c: Float
) {
    fun solveRealRoots(): List<Float> {
        val discriminant = b * b - 4 * a * c
        if (discriminant < 0f) return emptyList()
        val r1 = (-b + sqrt(discriminant)) / (2 * a)
        val r2 = (-b - sqrt(discriminant)) / (2 * a)
        return listOf(r1, r2)
    }
}
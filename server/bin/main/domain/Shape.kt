package domain

data class Shape(
    val id: Int,
    val mass: Float,
    val sideLength: Int,
    val data: List<Int>,
) {
    fun codedString(): String {
        return data.map { it.toChar() }.joinToString("")
    }
}

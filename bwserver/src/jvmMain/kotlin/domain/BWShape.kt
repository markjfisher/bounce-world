package domain

data class BWShape(
    override val id: Int,
    override val mass: Float,
    val sideLength: Int,
    val data: List<Int>
) : Shape {
    override val radius: Float
        get() = sideLength / 2f

    override fun codedString(): String {
        return data.map { it.toChar() }.joinToString("")
    }
}
package domain

interface Shape {
    val id: Int
    val mass: Float
    val radius: Float

    fun codedString(): String
}

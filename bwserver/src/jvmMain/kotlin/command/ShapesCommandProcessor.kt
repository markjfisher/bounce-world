package command

import domain.World

class ShapesCommandProcessor(private val world: World) {
    fun getShapesData(): ByteArray {
        return world.shapes.fold(mutableListOf<Byte>()) { acc, s ->
            acc.apply {
                add(s.id.toByte())
                add(s.sideLength.toByte())
                addAll(s.codedString().toByteArray().toList())
            }
        }.toByteArray()
    }

    fun getShapesCount(): Int = world.shapes.size
}
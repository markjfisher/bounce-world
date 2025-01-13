package command

import domain.World

class ShapesCommandProcessor(private val world: World) {
    fun getShapesData(): ByteArray {
        return world.getShapes().fold(mutableListOf<Byte>()) { acc, s ->
            acc.apply {
                add(s.id.toByte())
                add((s.radius * 2f).toInt().toByte())
                addAll(s.codedString().toByteArray().toList())
            }
        }.toByteArray()
    }

    fun getShapesCount(): Int = world.getShapes().size
}
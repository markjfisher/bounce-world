package data

import domain.BWShape
import domain.Shape
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ShapeCreator {
    private var nextShapeId: Int = 0
    private const val SHAPES_RESOURCE_PATH = "/shapes/"

//    @OptIn(ExperimentalStdlibApi::class)
    private fun fromString(s: String): List<Int> {
        val strippedString = s.replace("[\r\n]+".toRegex(), "")
        val sideLength = sqrt(strippedString.length.toFloat()).roundToInt()
        if (sideLength * sideLength != strippedString.length) {
            throw IllegalArgumentException("Invalid shape length $sideLength")
        }
        val mappedList = strippedString.map { c ->
            // convert to neutral char codes, each platform can convert back to its own char
            when(c) {
                '┌' -> 'r'.code
                '┐' -> ')'.code
                '└' -> 'L'.code
                '┘' -> '!'.code
                '┤' -> 'J'.code
                '├' -> 't'.code
                '┬' -> 'T'.code
                '┴' -> '2'.code
                '│' -> '|'.code
                '─' -> '-'.code
                '┼' -> '+'.code
                '▌' -> 'a'.code
                '▐' -> 'b'.code
                '▄' -> 'c'.code
                '▀' -> 'd'.code
                '▖' -> 'e'.code
                '▗' -> 'f'.code
                '▘' -> 'g'.code
                '▝' -> 'h'.code

                '▜' -> 'i'.code
                '▛' -> 'j'.code
                '▟' -> 'k'.code
                '▙' -> 'l'.code

                '█' -> 'm'.code

                // these don't have mappings in atascii, might avoid, but provided to allow platforms to use them
                '▚' -> 'n'.code
                '▞' -> 'p'.code

                else -> c.code
            }
        }
//        val hexBytes = mappedList.map { it.toByte() }
//            .toByteArray()
//            .toHexString()
//            .windowed(size = 2, step = 2, partialWindows = true)
//            .joinToString(separator = " ")
//        println("$s -> $hexBytes")

        return mappedList
    }

    private fun createShape(mass: Float, data: List<Int>): Shape {
        return BWShape(
            id = nextShapeId++,
            mass = mass,
            sideLength = sqrt(data.size.toDouble()).roundToInt(),
            data = data
        )
    }

    private fun readShapeFromResource(resourceName: String): Shape? {
        val stream = ShapeCreator::class.java.getResourceAsStream(SHAPES_RESOURCE_PATH + resourceName)
        stream?.bufferedReader().use { reader ->
            val lines = reader?.readLines() ?: return null

            val mass = lines.first().toFloat()
            val shapeString = lines.drop(1).joinToString(separator = "\n")

            return createShape(mass, fromString(shapeString))
        }
    }

    fun createShapes(): List<Shape> {
        val shapeResources = listOf(
            "shape01.dat",
            "shape02.dat",
            "shape03.dat",
            "shape04.dat",
            "shape05.dat",
            "shape06.dat",
            "shape07.dat",
//            "shape08.dat",
//            "shape09.dat",
            "shape10.dat",
            "shape11.dat",
            "shape12.dat",
//            "shape13.dat",
            "shape14.dat",
            "shape15.dat",
//            "shape16.dat",
//            "shape17.dat",
            "shape18.dat",
            "shape19.dat",
            "shape20.dat",
            "shape21.dat",
            "shape22.dat",
            "shape23.dat",
            "shape24.dat",
        )
        return shapeResources.mapNotNull { readShapeFromResource(it) }
    }
}
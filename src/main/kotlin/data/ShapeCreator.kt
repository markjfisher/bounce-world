package data

import domain.Shape
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ShapeCreator {
    private var nextShapeId: Int = 0

    @OptIn(ExperimentalStdlibApi::class)
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
        return Shape(
            id = nextShapeId++,
            mass = mass,
            sideLength = sqrt(data.size.toDouble()).roundToInt(),
            data = data
        )
    }

    fun createShapes(): List<Shape> {
        val shapes = mutableListOf<Shape>()
        shapes.addAll(listOf(
            createShape(5.8f, fromString(
                """
                    |  ┌┐ 
                    |┌─┘└┐
                    |└┐ ┌┘
                    | └┐│ 
                    |  └┘ 
                    """.trimMargin()
                )
            ),
//            createShape(4.6f, fromString(
//                """
//                    | ┌┐
//                    |┌┘└┐
//                    |└┐ └┐
//                    |┌┘ ┌┘
//                    |└──┘
//                    """.trimMargin()
//                )
//            ),
            createShape(4.0f, fromString(
                """
                    | ┌┐ 
                    |┌┘└┐
                    |└┐┌┘
                    | └┘ 
                    """.trimMargin()
                )
            ),
//            createShape(4.0f, fromString(
//                """
//                    |  ┌┐
//                    |┌─┘│
//                    |│┌─┘
//                    |└┘
//                    """.trimMargin()
//                )
//            ),
            createShape(4.1f, fromString(
                """
                    | \/ 
                    |\/\/
                    |/\/\
                    | /\ 
                    """.trimMargin()
            )
            ),
            createShape(3.6f, fromString(
                    """
                    | * 
                    |***
                    | * 
                    """.trimMargin()
                )
            ),
            createShape(3.6f, fromString(
                    """
                    |# #
                    | # 
                    |# #
                    """.trimMargin()
                )
            ),
//            createShape(4.0f, fromString(
//                    """
//                    | ▙
//                    |▟█▛
//                    | ▜
//                    """.trimMargin()
//                )
//            ),
//            createShape(4.0f, fromString(
//                    """
//                    | ▟
//                    |▜█▙
//                    | ▛
//                    """.trimMargin()
//                )
//            ),
            createShape(3.4f, fromString(
                    """
                    | ┌┐
                    |┌┼┘
                    |└┘ 
                    """.trimMargin()
                )
            ),
            createShape(3.4f, fromString(
                    """
                    |┌┐ 
                    |└┼┐
                    | └┘
                    """.trimMargin()
                )
            ),
            createShape(3.0f, fromString(
                    """
                    |/\
                    |\/
                    """.trimMargin()
                )
            ),
            createShape(3.0f, fromString(
                    """
                    |&X
                    |X&
                    """.trimMargin()
                )
            ),
            createShape(3.0f, fromString(
                    """
                    |WM
                    |MW
                    """.trimMargin()
                )
            ),
            createShape(3.0f, fromString(
                    """
                    |##
                    |##
                    """.trimMargin()
                )
            ),
            createShape(3.2f, fromString(
                    """
                    |▟▙
                    |▜▛
                    """.trimMargin()
                )
            ),
//            createShape(2.2f, fromString(
//                    """
//                    |┼
//                    """.trimMargin()
//                )
//            ),
            createShape(2.0f, fromString(
                    """
                    |*
                    """.trimMargin()
                )
            ),
            createShape(2.0f, fromString(
                    """
                    |O
                    """.trimMargin()
                )
            ),
            createShape(2.0f, fromString(
                    """
                    |#
                    """.trimMargin()
                )
            ),
            createShape(2.0f, fromString(
                    """
                    |X
                    """.trimMargin()
                )
            ),
            createShape(2.0f, fromString(
                    """
                    |@
                    """.trimMargin()
                )
            ),
            createShape(2.0f, fromString(
                    """
                    |Y
                    """.trimMargin()
                )
            ),
            createShape(2.0f, fromString(
                    """
                    |V
                    """.trimMargin()
                )
            ),
            createShape(2.0f, fromString(
                    """
                    |"
                    """.trimMargin()
                )
            ),
            createShape(2.0f, fromString(
                    """
                    |A
                    """.trimMargin()
                )
            ),
            createShape(2.0f, fromString(
                    """
                    |^
                    """.trimMargin()
                )
            ),
        ))
        return shapes
    }
}
package data

import domain.Shape
import kotlin.math.sqrt

object ShapeCreator {
    var nextShapeId: Int = 0

    private fun fromString(s: String): List<Int> {
        val strippedString = s.replace("[\r\n]+".toRegex(), "")
        val sideLength = sqrt(strippedString.length.toFloat()).toInt()
        if (sideLength * sideLength != strippedString.length) {
            throw IllegalArgumentException("Invalid shape length $sideLength")
        }
        return strippedString.map { c ->
            // convert to neutral char codes, each platform can convert back to its own char
            when(c) {
//                '/' -> 6
//                '\\' -> 7
                '┌' -> 'r'.code
                '┐' -> ')'.code
                '└' -> 'L'.code
                '┘' -> '!'.code
                '┤' -> 'J'.code
                '├' -> 't'.code
                '┬' -> 'T'.code
                '┴' -> '2'.code
                '│' -> '|'.code
//                '|' -> 2
                '─' -> '-'.code
//                '-' -> 18
                '┼' -> '+'.code
//                '+' -> 19
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

//                '<' -> ';'.code       // these need changing if sending via JSON response. otherwise fine.
//                '>' -> ':'.code
                else -> c.code
            }
        }
    }

    fun createShape(mass: Float, side: Int, data: List<Int>): Shape {
        return Shape(
            id = nextShapeId++,
            mass = mass,
            sideLength = side,
            data = data
        )
    }

    fun createShapes(): List<Shape> {
        val shapes = mutableListOf<Shape>()
        shapes.addAll(listOf(
            createShape(4.4f, 5, fromString(
                """
                | ┬┬  
                |└┤├┼┤
                | ┼  ┼
                |┌┤ ├┤
                | ┼┴┴ 
                """.trimMargin()
            )),
            createShape(4.5f, 5, fromString(
                """
                | \/  
                |\/\/ 
                |/\/\/
                | /\/\
                |  /\ 
                """.trimMargin()
            )),
            createShape(.5f, 5, fromString(
                """
                |  ▄  
                | / \ 
                |▐ # ▌
                | \ / 
                |  ▀  
                """.trimMargin()
            )),
            createShape(4.3f, 5, fromString(
                """
                |  ┌┐ 
                |┌─┘└┐
                |└┐ ┌┘
                | └┐│ 
                |  └┘ 
                """.trimMargin()
            )),
            createShape(4.3f, 5, fromString(
                """
                |  ┬  
                | ┌┼┐ 
                |├┼ ┼┤
                | └┼┘ 
                |  ┴  
                """.trimMargin()
            )),
            createShape(3.8f, 3, fromString(
                """
                | * 
                |* *
                | * 
                """.trimMargin()
            )),
            createShape(3.8f, 3, fromString(
                """
                | # 
                |#O#
                | # 
                """.trimMargin()
            )),
            createShape(3.7f, 3, fromString(
                """
                | ─ 
                |│X│
                | ─ 
                """.trimMargin()
            )),
            createShape(4.1f, 3, fromString(
                """
                | ▙ 
                |▟█▛
                | ▜ 
                """.trimMargin()
            )),
            createShape(4.1f, 3, fromString(
                """
                | ▟ 
                |▜█▙
                | ▛ 
                """.trimMargin()
            )),
            createShape(3.7f, 3, fromString(
                """
                | ┌┐
                |┌┼┘
                |└┘ 
                """.trimMargin()
            )),
            createShape(3.7f, 3, fromString(
                """
                |┌┐ 
                |└┼┐
                | └┘
                """.trimMargin()
            )),
            createShape(3.6f, 2, fromString(
                """
                |/\
                |\/
                """.trimMargin()
            )),
            createShape(3.6f, 2, fromString(
                """
                |┌┐
                |└┘
                """.trimMargin()
            )),
            createShape(3.7f, 2, fromString(
                """
                |▟▙
                |▜▛
                """.trimMargin()
            )),
            createShape(3.6f, 2, fromString(
                """
                |▗▖
                |▜▛
                """.trimMargin()
            )),
            createShape(3.3f, 1, fromString(
                """
                |┼
                """.trimMargin()
            )),
            createShape(3.3f, 1, fromString(
                """
                |*
                """.trimMargin()
            )),
            createShape(3.3f, 1, fromString(
                """
                |O
                """.trimMargin()
            )),
            createShape(3.3f, 1, fromString(
                """
                |#
                """.trimMargin()
            )),
            createShape(3.3f, 1, fromString(
                """
                |X
                """.trimMargin()
            )),
        ))
        return shapes
    }
}
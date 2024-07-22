package data

import domain.Shape
import kotlin.math.sqrt

object ShapeCreator {
    private fun fromString(s: String): List<Int> {
        val strippedString = s.replace("[\r\n]+".toRegex(), "")
        val sideLength = sqrt(strippedString.length.toFloat()).toInt()
        if (sideLength * sideLength != strippedString.length) {
            throw IllegalArgumentException("Invalid shape length $sideLength")
        }
        return strippedString.map { c ->
            // convert to atascii codes, each platform can convert back to its own char
            when(c) {
                '/' -> 6
                '\\' -> 7
                '┌' -> 17
                '┐' -> 5
                '└' -> 26
                '┘' -> 3
                '┤' -> 4
                '├' -> 1
                '┬' -> 23
                '┴' -> 24
                '│' -> 2
                '|' -> 2
                '─' -> 18
                '-' -> 18
                '┼' -> 19
                '+' -> 19
                '▌' -> 25
                '▐' -> 153 // inverted above (+128)
                '▄' -> 21
                '▀' -> 149 // inverted above (+128)
                '▖' -> 15
                '▗' -> 9
                '▘' -> 12
                '▝' -> 11
                else -> c.code
            }
        }
    }

    fun createShapes(): List<Shape> {
        val shapes = mutableListOf<Shape>()
        shapes.addAll(listOf(
            Shape(0, 4f, 3, fromString(
                """
                | * 
                |* *
                | * 
                """.trimMargin()
            )),
            Shape(1, 4.1f, 3, fromString(
                """
                |▗▄▖
                |▐ ▌
                |▝▀▘
                """.trimMargin()
            )),
            Shape(2, 4f, 3, fromString(
                """
                | # 
                |#O#
                | # 
                """.trimMargin()
            )),
            Shape(3, 4f, 3, fromString(
                """
                | ─ 
                |│X│
                | ─ 
                """.trimMargin()
            )),
            Shape(4, 3.8f, 3, fromString(
                """
                | n 
                | * 
                | u 
                """.trimMargin()
            )),
            Shape(5, 3.8f, 3, fromString(
                """
                |   
                |<O>
                |   
                """.trimMargin()
            )),
            Shape(6, 4f, 3, fromString(
                """
                | ┌┐
                |┌┼┘
                |└┘ 
                """.trimMargin()
            )),
            Shape(7, 4.4f, 5, fromString(
                """
                | ┬┬  
                |└┤├┼┤
                | ┼  ┼
                |┌┤ ├┤
                | ┼┴┴ 
                """.trimMargin()
            )),
            Shape(8, 4.5f, 5, fromString(
                """
                | \/  
                |\/\/ 
                |/\/\/
                | /\/\
                |  /\ 
                """.trimMargin()
            )),
            Shape(9,4.5f, 5, fromString(
                """
                |  ▄  
                | /$\ 
                |<#O#>
                | \$/ 
                |  ▀  
                """.trimMargin()
            )),
            Shape(10, 4.3f, 5, fromString(
                """
                |  ┌┐ 
                |┌─┘└┐
                |└┐ ┌┘
                | └┐│ 
                |  └┘ 
                """.trimMargin()
            )),
            Shape(11, 4.3f, 5, fromString(
                """
                |  ┬  
                | ┌┼┐ 
                |├┼ ┼┤
                | └┼┘ 
                |  ┴  
                """.trimMargin()
            )),
            Shape(12, 3.6f, 2, fromString(
                """
                |/\
                |\/
                """.trimMargin()
            )),
            Shape(13, 3.6f, 2, fromString(
                """
                |┌┐
                |└┘
                """.trimMargin()
            )),
            Shape(14, 3.7f, 2, fromString(
                """
                |##
                |##
                """.trimMargin()
            )),
            Shape(15, 3.5f, 2, fromString(
                """
                | *
                |* 
                """.trimMargin()
            )),
            Shape(16, 3.5f, 2, fromString(
                """
                |+ 
                | +
                """.trimMargin()
            )),
            Shape(17, 3.6f, 2, fromString(
                """
                |%/
                |/%
                """.trimMargin()
            )),
            Shape(18, 3.3f, 3, fromString(
                """
                |┼
                """.trimMargin()
            )),
            Shape(19, 3.3f, 1, fromString(
                """
                |*
                """.trimMargin()
            )),
            Shape(20, 3.3f, 1, fromString(
                """
                |O
                """.trimMargin()
            )),
            Shape(21, 3.3f, 1, fromString(
                """
                |#
                """.trimMargin()
            )),
            Shape(22, 3.3f, 1, fromString(
                """
                |X
                """.trimMargin()
            )),
            Shape(23, 3.3f, 1, fromString(
                """
                |%
                """.trimMargin()
            )),
            Shape(24, 3.3f, 1, fromString(
                """
                |0
                """.trimMargin()
            )),
            Shape(25, 3.3f, 1, fromString(
                """
                |$
                """.trimMargin()
            )),
            Shape(26, 3.3f, 1, fromString(
                """
                |=
                """.trimMargin()
            )),
        ))
        return shapes
    }
}
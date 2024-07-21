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
            Shape(1, 6f, 3, fromString(
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
            Shape(4, 3f, 3, fromString(
                """
                | n 
                | * 
                | u 
                """.trimMargin()
            )),
            Shape(5, 3f, 3, fromString(
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
            Shape(7, 9f, 5, fromString(
                """
                | ┬┬  
                |└┤├┼┤
                | ┼  ┼
                |┌┤ ├┤
                | ┼┴┴ 
                """.trimMargin()
            )),
            Shape(8, 9f, 5, fromString(
                """
                | \/  
                |\/\/ 
                |/\/\/
                | /\/\
                |  /\ 
                """.trimMargin()
            )),
            Shape(9,10f, 5, fromString(
                """
                |  ▄  
                | /$\ 
                |<#O#>
                | \$/ 
                |  ▀  
                """.trimMargin()
            )),
            Shape(10, 7f, 5, fromString(
                """
                |  ┌┐ 
                |┌─┘└┐
                |└┐ ┌┘
                | └┐│ 
                |  └┘ 
                """.trimMargin()
            )),
            Shape(11, 7f, 5, fromString(
                """
                |  ┬  
                | ┌┼┐ 
                |├┼ ┼┤
                | └┼┘ 
                |  ┴  
                """.trimMargin()
            )),
            Shape(12, 3f, 2, fromString(
                """
                |/\
                |\/
                """.trimMargin()
            )),
            Shape(13, 2f, 2, fromString(
                """
                |┌┐
                |└┘
                """.trimMargin()
            )),
            Shape(14, 3f, 2, fromString(
                """
                |##
                |##
                """.trimMargin()
            )),
            Shape(15, 2f, 2, fromString(
                """
                | *
                |* 
                """.trimMargin()
            )),
            Shape(16, 2f, 2, fromString(
                """
                |+ 
                | +
                """.trimMargin()
            )),
            Shape(17, 3f, 2, fromString(
                """
                |%/
                |/%
                """.trimMargin()
            )),
            Shape(18, 6f, 3, fromString(
                """
                |┼
                """.trimMargin()
            )),
            Shape(19, 1f, 1, fromString(
                """
                |*
                """.trimMargin()
            )),
            Shape(20, 1f, 1, fromString(
                """
                |O
                """.trimMargin()
            )),
            Shape(21, 1f, 1, fromString(
                """
                |#
                """.trimMargin()
            )),
            Shape(22, 1f, 1, fromString(
                """
                |X
                """.trimMargin()
            )),
            Shape(23, 1f, 1, fromString(
                """
                |%
                """.trimMargin()
            )),
            Shape(24, 1f, 1, fromString(
                """
                |0
                """.trimMargin()
            )),
            Shape(25, 1f, 1, fromString(
                """
                |$
                """.trimMargin()
            )),
            Shape(26, 1f, 1, fromString(
                """
                |=
                """.trimMargin()
            )),
        ))
        return shapes
    }
}
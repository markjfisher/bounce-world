package geometry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainInOrder

class DiamondPatternGeneratorTest : StringSpec({
    "can iterate" {
        val generator = DiamondPatternGenerator()
        val values = generator.generate().take(16).toList()
        values.shouldContainInOrder(
            Point(x = 0, y = 0),
            Point(x = 1, y = 0),
            Point(x = 0, y = 1),
            Point(x = -1, y = 0),
            Point(x = 0, y = -1),
            Point(x = 2, y = 0),
            Point(x = 1, y = 1),
            Point(x = 0, y = 2),
            Point(x = -1, y = 1),
            Point(x = -2, y = 0),
            Point(x = -1, y = -1),
            Point(x = 0, y = -2),
            Point(x = 1, y = -1),
            Point(x = 3, y = 0),
            Point(x = 2, y = 1),
            Point(x = 1, y = 2)
        )
    }
})
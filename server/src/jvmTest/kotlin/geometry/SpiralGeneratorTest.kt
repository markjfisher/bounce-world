package geometry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainInOrder

class SpiralGeneratorTest : StringSpec({
    "can generate spiral points" {
        val generator = SpiralGenerator()
        val points = generator.generate().take(16).toList()
        points.shouldContainInOrder(
            Point(x=0, y=0),
            Point(x=1, y=0),
            Point(x=1, y=1),
            Point(x=0, y=1),
            Point(x=-1, y=1),
            Point(x=-1, y=0),
            Point(x=-1, y=-1),
            Point(x=0, y=-1),
            Point(x=1, y=-1),
            Point(x=2, y=-1),
            Point(x=2, y=0),
            Point(x=2, y=1),
            Point(x=2, y=2),
            Point(x=1, y=2),
            Point(x=0, y=2),
            Point(x=-1, y=2)
        )
    }

})
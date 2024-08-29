package geometry

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SpiralGeneratorTest {
    @Test
    fun `can generate spiral points`() {
        val generator = SpiralGenerator()
        val points = generator.generate().take(16).toList()
        assertThat(points).containsExactly(
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

}
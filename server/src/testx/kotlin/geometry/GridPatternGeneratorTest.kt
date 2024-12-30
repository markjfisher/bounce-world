package geometry

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GridPatternGeneratorTest {
    @Test
    fun `can iterate`() {
        val generator = GridPatternGenerator()
        val values = generator.generate().take(16).toList()
        assertThat(values).containsExactly(
            Point(x=0, y=0),
            Point(x=1, y=0),
            Point(x=1, y=1),
            Point(x=0, y=1),
            Point(x=2, y=0),
            Point(x=2, y=1),
            Point(x=2, y=2),
            Point(x=1, y=2),
            Point(x=0, y=2),
            Point(x=3, y=0),
            Point(x=3, y=1),
            Point(x=3, y=2),
            Point(x=3, y=3),
            Point(x=2, y=3),
            Point(x=1, y=3),
            Point(x=0, y=3)
        )
    }
}
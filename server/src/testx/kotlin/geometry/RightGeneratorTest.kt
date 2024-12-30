package geometry


import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RightGeneratorTest {
    @Test
    fun `can generate rightward points`() {
        val generator = RightGenerator()
        val points = generator.generate().take(16).toList()
        assertThat(points).containsExactly(
            Point(x=0, y=0),
            Point(x=1, y=0),
            Point(x=2, y=0),
            Point(x=3, y=0),
            Point(x=4, y=0),
            Point(x=5, y=0),
            Point(x=6, y=0),
            Point(x=7, y=0),
            Point(x=8, y=0),
            Point(x=9, y=0),
            Point(x=10, y=0),
            Point(x=11, y=0),
            Point(x=12, y=0),
            Point(x=13, y=0),
            Point(x=14, y=0),
            Point(x=15, y=0)
        )
    }

}
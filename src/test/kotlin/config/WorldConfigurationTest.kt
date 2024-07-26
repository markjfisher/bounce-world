package config

import io.micronaut.context.ApplicationContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WorldConfigurationTest {
    @Test
    fun `world configuration loads`() {
        val items = mapOf(
            "world.width" to "100",
            "world.height" to "50"
        )

        val ctx = ApplicationContext.run(items)
        val worldConfiguration = ctx.getBean(WorldConfiguration::class.java)

        assertThat(worldConfiguration.width).isEqualTo(100)
        assertThat(worldConfiguration.height).isEqualTo(50)
        ctx.close()
    }
}
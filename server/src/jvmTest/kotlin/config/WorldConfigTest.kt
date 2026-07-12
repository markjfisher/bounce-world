package config

import io.kotest.matchers.shouldBe
import io.ktor.server.config.ApplicationConfig
import org.junit.jupiter.api.Test

class WorldConfigTest {
    @Test
    fun `loads tcp read timeout from application config`() {
        val config = WorldConfig(ApplicationConfig("application.conf"))

        config.tcpReadTimeoutMillis shouldBe 30_000L
    }
}

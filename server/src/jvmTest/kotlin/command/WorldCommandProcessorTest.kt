package command

import config.WorldConfig
import domain.GameClientInfo
import domain.ScreenSize
import domain.VisibleShape
import factory.WorldFactory
import geometry.Point
import io.kotest.matchers.shouldBe
import io.ktor.server.config.MapApplicationConfig
import org.junit.jupiter.api.Test

class WorldCommandProcessorTest {
    private val defaultWorldApplicationConfig = MapApplicationConfig(
        "world.width" to "40",
        "world.height" to "24",
        "world.updatesPerSecond" to "5",
        "world.shouldAutoStart" to "false",
        "world.initialSpeed" to "1.5",
        "world.heartbeatTimeoutMillis" to "10000",
        "world.locationPattern" to "grid",
        "world.enableWrapping" to "true",
        "world.loggingRequests" to "false",
        "world.tcp.host" to "0.0.0.0",
        "world.tcp.port" to "9002",
    )

    private fun processor(): Pair<WorldCommandProcessor, Int> {
        val config = WorldConfig(defaultWorldApplicationConfig)
        val world = WorldFactory.create(config)
        val client = world.createClient(
            GameClientInfo(
                name = "atari",
                version = 2,
                screenSize = ScreenSize(40, 24),
            ),
        )
        world.currentClientVisibleShapes[client.id] = mutableSetOf(
            VisibleShape(shapeId = 1, position = Point(10, 10), bodyId = 1),
            VisibleShape(shapeId = 2, position = Point(20, 12), bodyId = 2),
        )
        return WorldCommandProcessor(world, config) to client.id
    }

    @Test
    fun `prependPacketSize writes little-endian total packet size`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03)

        WorldCommandProcessor.prependPacketSize(payload) shouldBe byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x03)
    }

    @Test
    fun `getWorldDataWithSize prefixes total packet size for unknown client`() {
        val (wcp, _) = processor()

        wcp.getWorldDataWithSize(99) shouldBe byteArrayOf(0x03, 0x00, 0x00)
    }

    @Test
    fun `getWorldDataWithSize payload matches getWorldData`() {
        val (wcp, clientId) = processor()
        val payload = wcp.getWorldData(clientId)
        val response = wcp.getWorldDataWithSize(clientId)

        response.size shouldBe payload.size + 2
        response[0].toUByte().toInt() shouldBe (response.size and 0xFF)
        response[1].toUByte().toInt() shouldBe ((response.size shr 8) and 0xFF)
        response.copyOfRange(2, response.size) shouldBe payload
    }
}

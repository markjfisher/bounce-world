package server

import command.ClientCommandProcessor
import command.ShapesCommandProcessor
import command.WorldCommandProcessor
import config.WorldConfig
import domain.GameClientInfo
import domain.ScreenSize
import domain.VisibleShape
import factory.WorldFactory
import geometry.Point
import io.kotest.matchers.shouldBe
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test

class TcpServerCommandTest {
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

    private fun tcpServer(): Pair<TcpServer, Int> {
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
        )
        val wcp = WorldCommandProcessor(world, config)
        val server = TcpServer(
            wcp,
            ClientCommandProcessor(world),
            ShapesCommandProcessor(world),
            "127.0.0.1",
            0,
            false,
            CoroutineScope(Dispatchers.Unconfined),
        )
        return server to client.id
    }

    @Test
    fun `d command returns world data with size prefix`() {
        val (server, clientId) = tcpServer()
        val payload = server.processCommand("w $clientId")
        val response = server.processCommand("d $clientId")

        response.size shouldBe payload.size + 2
        response.copyOfRange(2, response.size) shouldBe payload
    }

    @Test
    fun `d command with invalid client id returns size-prefixed error`() {
        val (server, _) = tcpServer()

        server.processCommand("d not-a-number") shouldBe byteArrayOf(0x03, 0x00, 0x00)
    }
}

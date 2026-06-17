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
        "world.tcp.framed.port" to "9003",
    )

    private fun tcpServer(prependResponseSize: Boolean = false): Pair<TcpServer, Int> {
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
            if (prependResponseSize) config.tcpFramedPort else config.tcpPort,
            false,
            prependResponseSize,
            CoroutineScope(Dispatchers.Unconfined),
        )
        return server to client.id
    }

    @Test
    fun `prependPacketSize writes little-endian total packet size`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03)

        TcpServer.prependPacketSize(payload) shouldBe byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x03)
    }

    @Test
    fun `legacy server returns raw payload`() {
        val (server, clientId) = tcpServer(prependResponseSize = false)
        val payload = server.processCommand("w $clientId")

        server.formatResponse(payload) shouldBe payload
    }

    @Test
    fun `framed server wraps world data response`() {
        val (server, clientId) = tcpServer(prependResponseSize = true)
        val payload = server.processCommand("w $clientId")
        val response = server.formatResponse(payload)

        response.size shouldBe payload.size + 2
        response[0].toUByte().toInt() shouldBe (response.size and 0xFF)
        response[1].toUByte().toInt() shouldBe ((response.size shr 8) and 0xFF)
        response.copyOfRange(2, response.size) shouldBe payload
    }

    @Test
    fun `framed server wraps all command responses`() {
        val (server, clientId) = tcpServer(prependResponseSize = true)

        listOf(
            "shape-count",
            "ws",
            "w $clientId",
            "freeze",
        ).forEach { command ->
            val payload = server.processCommand(command)
            val response = server.formatResponse(payload)

            response.size shouldBe payload.size + 2
            response.copyOfRange(2, response.size) shouldBe payload
        }
    }

    @Test
    fun `framed server wraps error responses`() {
        val (server, _) = tcpServer(prependResponseSize = true)
        val payload = server.processCommand("w not-a-number")

        server.formatResponse(payload) shouldBe byteArrayOf(0x03, 0x00, 0x00)
    }
}

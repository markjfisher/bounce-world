package server

import command.ClientCommandProcessor
import command.ShapesCommandProcessor
import command.WorldCommandProcessor
import config.WorldConfig
import domain.ClientCapabilities
import domain.GameClient
import domain.GameClientInfo
import domain.ScreenSize
import domain.VisibleShape
import domain.World
import factory.WorldFactory
import geometry.Point
import io.kotest.matchers.shouldBe
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import org.joml.Vector2f
import java.lang.Math.abs
import kotlin.math.roundToInt

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
        "world.tcp.readTimeoutMillis" to "30000",
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
            VisibleShape(shapeId = 1, position = Vector2f(10f, 10f), bodyId = 1),
        )
        val wcp = WorldCommandProcessor(world, config)
        val server = TcpServer(
            wcp,
            ClientCommandProcessor(world),
            ShapesCommandProcessor(world),
            "127.0.0.1",
            if (prependResponseSize) config.tcpFramedPort else config.tcpPort,
            false,
            config.tcpReadTimeoutMillis,
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

    @Test
    fun `add-client accepts hex and decimal capability values of any width`() {
        val (server, _) = tcpServer()

        // high bits well past any current feature; both forms must register successfully
        listOf("0x8000", "32768", "0x10000", "65536").forEach { caps ->
            val response = server.processCommand("add-client caps-test,1,40,24,40,24,$caps")
            response[0].toInt() shouldBeGreaterThan 0
        }
    }

    @Test
    fun `add-client rejects negative or non-numeric capabilities`() {
        val (server, _) = tcpServer()

        listOf("-1", "not-a-number").forEach { caps ->
            server.processCommand("add-client caps-test,1,40,24,40,24,$caps") shouldBe byteArrayOf(0)
        }
    }

    private fun worldWithClient(version: Int, capabilities: Int = 0): Triple<World, GameClient, WorldCommandProcessor> {
        val config = WorldConfig(defaultWorldApplicationConfig)
        val world = WorldFactory.create(config)
        val client = world.createClient(
            GameClientInfo(
                name = "atari",
                version = version,
                screenSize = ScreenSize(320, 256),
                worldSize = ScreenSize(320, 256),
                capabilities = capabilities,
            ),
        )
        return Triple(world, client, WorldCommandProcessor(world, config))
    }

    @Test
    fun `v2 clients get single byte coordinates`() {
        val (world, client, wcp) = worldWithClient(version = 2)
        world.currentClientVisibleShapes[client.id] = mutableSetOf(
            VisibleShape(shapeId = 7, position = Vector2f(100f, 50f), bodyId = 1),
        )

        val data = wcp.asBinary(client.id)

        data.size shouldBe 4
        data[0] shouldBe 1
        data[1] shouldBe 7
        data[2].toUByte().toInt() shouldBe 100
        data[3].toUByte().toInt() shouldBe 50
    }

    @Test
    fun `v3 clients without wide coords capability get single byte coordinates`() {
        val (world, client, wcp) = worldWithClient(version = 3)
        world.currentClientVisibleShapes[client.id] = mutableSetOf(
            VisibleShape(shapeId = 7, position = Vector2f(300f, 200f), bodyId = 1),
        )

        val data = wcp.asBinary(client.id)

        // version alone no longer grants anything; legacy layout applies
        data.size shouldBe 4
        data[2].toUByte().toInt() shouldBe 44 // 300 wraps in a single byte
    }

    @Test
    fun `clients with wide coords capability get little-endian two byte coordinates`() {
        val (world, client, wcp) = worldWithClient(version = 2, capabilities = ClientCapabilities.WIDE_COORDS)
        world.currentClientVisibleShapes[client.id] = mutableSetOf(
            VisibleShape(shapeId = 7, position = Vector2f(300f, 200f), bodyId = 1),
        )

        val data = wcp.asBinary(client.id)

        data.size shouldBe 6
        data[0] shouldBe 1
        data[1] shouldBe 7
        // 300 = 0x012C and 200 = 0x00C8, little-endian shorts
        data[2].toUByte().toInt() shouldBe (300 and 0xFF)
        data[3].toUByte().toInt() shouldBe (300 shr 8)
        data[4].toUByte().toInt() shouldBe (200 and 0xFF)
        data[5].toUByte().toInt() shouldBe (200 shr 8)
    }

    @Test
    fun `clients with rotation capability get angle and angular velocity appended`() {
        val (world, client, wcp) = worldWithClient(
            version = 2,
            capabilities = ClientCapabilities.WIDE_COORDS or ClientCapabilities.ROTATION,
        )
        // angle pi/2 -> quarter of a turn = 16383.75 bits; omega -1.5 rad/s * 256 = -384
        world.currentClientVisibleShapes[client.id] = mutableSetOf(
            VisibleShape(shapeId = 9, position = Vector2f(10f, 10f), bodyId = 1,
                angle = (Math.PI / 2).toFloat(), angularVelocity = -1.5f),
        )

        val data = wcp.asBinary(client.id)

        // count + shapeId + x(2) + y(2) + angle(2) + omega(2)
        data.size shouldBe 10
        data[0] shouldBe 1
        data[1] shouldBe 9
        val x = data[2].toUByte().toInt() or (data[3].toUByte().toInt() shl 8)
        val y = data[4].toUByte().toInt() or (data[5].toUByte().toInt() shl 8)
        // screen size == world size in this fixture, so scale is 1.0
        x shouldBe 10
        y shouldBe 10

        val angleBits = data[6].toUByte().toInt() or (data[7].toUByte().toInt() shl 8)
        val expectedAngleBits = (((Math.PI / 2) / (2 * Math.PI)) * 65535.0).toFloat()
        abs(angleBits - expectedAngleBits) shouldBeLessThan 1.0f
        val omegaBits = (data[8].toUByte().toInt() or (data[9].toUByte().toInt() shl 8)).toShort().toInt()
        omegaBits shouldBe -384
    }

    @Test
    fun `fractional world positions scale to sub-world-unit pixels`() {
        val config = WorldConfig(defaultWorldApplicationConfig)
        val world = WorldFactory.create(config)
        val client = world.createClient(
            GameClientInfo(
                name = "hires",
                version = 3,
                screenSize = ScreenSize(320, 192),
                worldSize = ScreenSize(40, 24),
                capabilities = ClientCapabilities.WIDE_COORDS,
            ),
        )
        // 15.187 world units * 8 px-per-unit = 121.496 -> 121, not the quantised 15 * 8 = 120
        world.currentClientVisibleShapes[client.id] = mutableSetOf(
            VisibleShape(shapeId = 3, position = Vector2f(15.187f, 3.713f), bodyId = 1),
        )

        val data = wcpFor(world).asBinary(client.id)

        data.size shouldBe 6
        val x = (data[2].toUByte().toInt()) or (data[3].toUByte().toInt() shl 8)
        val y = (data[4].toUByte().toInt()) or (data[5].toUByte().toInt() shl 8)
        x shouldBe 121
        y shouldBe ((3.713f * (192f / 24f)).roundToInt())
    }

    private fun wcpFor(world: World) = WorldCommandProcessor(world, WorldConfig(defaultWorldApplicationConfig))
}

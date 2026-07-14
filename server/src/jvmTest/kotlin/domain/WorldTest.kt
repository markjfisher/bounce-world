package domain

import config.WorldConfig
import factory.WorldFactory
import geometry.Point
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.ktor.server.config.MapApplicationConfig
import io.mockk.mockk
import org.joml.Vector2f
import simulator.WorldSimulator

class WorldTest : StringSpec({
    val simulator: WorldSimulator = mockk(relaxed = true)
    val boundSimulator: WorldSimulator = mockk(relaxed = true)

    val defaultWorldApplicationConfig = MapApplicationConfig(
        "world.width" to "200",
        "world.height" to "200",
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
        "world.tcp.framed.port" to "9003"
    )

    val config = WorldConfig(defaultWorldApplicationConfig)

    fun worldConfig(locationPattern: String = "grid", enableWrapping: Boolean = true) = WorldConfig(
        MapApplicationConfig(
            "world.width" to "40",
            "world.height" to "24",
            "world.updatesPerSecond" to "5",
            "world.shouldAutoStart" to "false",
            "world.initialSpeed" to "1.5",
            "world.heartbeatTimeoutMillis" to "10000",
            "world.locationPattern" to locationPattern,
            "world.enableWrapping" to enableWrapping.toString(),
            "world.loggingRequests" to "false",
            "world.tcp.host" to "0.0.0.0",
            "world.tcp.port" to "9002",
            "world.tcp.readTimeoutMillis" to "30000",
            "world.tcp.framed.port" to "9003"
        )
    )

    "can add clients" {
        val world = World(config, simulator, boundSimulator)
        val c1 = world.createClient(GameClientInfo(name = "Client 1"))
        val c2 = world.createClient(GameClientInfo(name = "Client 2"))

        world.getClient(c1.id)?.id shouldBe c1.id
        world.getClient(c1.id)?.name shouldBe "Client 1"
        world.getClient(c1.id)?.position shouldBe Point(0, 0)

        world.getClient(c2.id)?.id shouldBe c2.id
        world.getClient(c2.id)?.name shouldBe "Client 2"
        world.getClient(c2.id)?.position shouldBe Point(1, 0)

        // no client with ID 69
        world.getClient(69) shouldBe null

        // check which client at which coordinates
        world.at(Point(0, 0))?.id shouldBe c1.id
        world.at(Point(1, 0))?.id shouldBe c2.id

        // no client at other locations
        world.at(Point(5, 5)) shouldBe null
    }

    "should allow removing client and putting new client in vacated position" {
        val world = World(config, simulator, boundSimulator)
        world.createClient(GameClientInfo(name = "Client 1"))
        val c2 = world.createClient(GameClientInfo(name = "Client 2"))
        world.createClient(GameClientInfo(name = "Client 3"))

        // remove client 2, thus freeing up the 1,0 slot
        world.removeClient(c2.id)
        world.getClient(c2.id) shouldBe null
        world.at(Point(1, 0)) shouldBe null

        // add a new client and ensure it was in the free slot at 1,0
        val c4 = world.createClient(GameClientInfo(name = "Client 4"))
        world.getClient(c4.id)!!.position shouldBe Point(1,0)
    }

    "boundary size stretches to maximum rectangle to contain all clients and is 1 based" {
        val world = World(config, simulator, boundSimulator)
        world.createClient(GameClientInfo(name = "Client 1"))
        world.worldBoundary() shouldBe Point(1,1)
        world.createClient(GameClientInfo(name = "Client 2"))
        world.worldBoundary() shouldBe Point(2,1)
        world.createClient(GameClientInfo(name = "Client 3"))
        world.worldBoundary() shouldBe Point(2,2)
        world.createClient(GameClientInfo(name = "Client 4"))
        world.worldBoundary() shouldBe Point(2,2)
        world.createClient(GameClientInfo(name = "Client 5"))
        world.worldBoundary() shouldBe Point(3,2)
        world.createClient(GameClientInfo(name = "Client 6"))
        world.worldBoundary() shouldBe Point(3,2)
        world.createClient(GameClientInfo(name = "Client 7"))
        world.worldBoundary() shouldBe Point(3,3)
        world.createClient(GameClientInfo(name = "Client 8"))
        world.worldBoundary() shouldBe Point(3,3)
        world.createClient(GameClientInfo(name = "Client 9"))
        world.worldBoundary() shouldBe Point(3,3)
    }

    "world boundary size with no clients has size 1,1" {
        val world = World(config, simulator, boundSimulator)
        world.worldBoundary() shouldBe Point(1,1)
    }

    "right layout uses each client's requested world width and the tallest current client" {
        val world = World(worldConfig(locationPattern = "right"), simulator, boundSimulator)

        val c1 = world.createClient(GameClientInfo(name = "Client 1"))
        val c2 = world.createClient(
            GameClientInfo(
                name = "Client 2",
                screenSize = ScreenSize(80, 24),
                worldSize = ScreenSize(80, 24),
            )
        )
        val c3 = world.createClient(
            GameClientInfo(
                name = "Client 3",
                screenSize = ScreenSize(200, 120),
                worldSize = ScreenSize(200, 120),
            )
        )

        c1.region shouldBe ClientRegion(0, 0, 40, 24)
        c2.region shouldBe ClientRegion(40, 0, 80, 24)
        c3.region shouldBe ClientRegion(120, 0, 200, 120)
        world.getWorldWidth() shouldBe 320
        world.getWorldHeight() shouldBe 120
    }

    "grid layout uses maximum client world dimensions per column and row" {
        val world = World(worldConfig(locationPattern = "grid"), simulator, boundSimulator)

        val c1 = world.createClient(GameClientInfo(name = "Client 1"))
        val c2 = world.createClient(
            GameClientInfo(
                name = "Client 2",
                screenSize = ScreenSize(80, 24),
                worldSize = ScreenSize(80, 24),
            )
        )
        val c3 = world.createClient(
            GameClientInfo(
                name = "Client 3",
                screenSize = ScreenSize(50, 60),
                worldSize = ScreenSize(50, 60),
            )
        )

        c1.position shouldBe Point(0, 0)
        c2.position shouldBe Point(1, 0)
        c3.position shouldBe Point(1, 1)
        c1.region shouldBe ClientRegion(0, 0, 40, 24)
        c2.region shouldBe ClientRegion(40, 0, 80, 24)
        c3.region shouldBe ClientRegion(40, 24, 50, 60)
        world.getWorldWidth() shouldBe 120
        world.getWorldHeight() shouldBe 84
    }

    "reused right slot recalculates world dimensions from the new client's world size" {
        val world = World(worldConfig(locationPattern = "right"), simulator, boundSimulator)
        world.createClient(GameClientInfo(name = "Client 1"))
        val wide = world.createClient(
            GameClientInfo(
                name = "Wide",
                screenSize = ScreenSize(200, 24),
                worldSize = ScreenSize(200, 24),
            )
        )

        world.getWorldWidth() shouldBe 240

        world.removeClient(wide.id)
        val narrower = world.createClient(
            GameClientInfo(
                name = "Narrower",
                screenSize = ScreenSize(80, 24),
                worldSize = ScreenSize(80, 24),
            )
        )

        narrower.position shouldBe Point(1, 0)
        narrower.region shouldBe ClientRegion(40, 0, 80, 24)
        world.getWorldWidth() shouldBe 120
    }

    "shrinking the bounded world clamps existing bodies inside the new dimensions" {
        val world = WorldFactory.create(worldConfig(locationPattern = "right", enableWrapping = false))
        world.createClient(GameClientInfo(name = "Client 1"))
        val tall = world.createClient(
            GameClientInfo(
                name = "Tall",
                screenSize = ScreenSize(200, 120),
                worldSize = ScreenSize(200, 120),
            )
        )
        val body = Body(
            id = 1,
            position = Vector2f(100f, 100f),
            velocity = Vector2f(1f, 1f),
            mass = 1f,
            radius = 1f,
            shapeId = 1,
        )

        world.currentSimulator.addBodies(listOf(body))
        world.removeClient(tall.id)

        val resizedBody = world.currentSimulator.mapBodies { it }.single()
        resizedBody.position.x shouldBeLessThanOrEqualTo 39f
        resizedBody.position.y shouldBeLessThanOrEqualTo 23f
    }
})

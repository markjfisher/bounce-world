package domain

import config.WorldConfig
import geometry.Point
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.config.MapApplicationConfig
import io.mockk.mockk
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
        "world.tcp.host" to "0.0.0.0",
        "world.tcp.port" to "9002"
    )

    val config = WorldConfig(defaultWorldApplicationConfig)

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
})

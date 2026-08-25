package domain

import config.WorldConfig
import factory.WorldFactory
import geometry.Point
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.ktor.server.config.MapApplicationConfig
import io.mockk.mockk
import org.joml.Vector2f
import simulator.BaseBodySimulator
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

    "client ids recycle the lowest free id after a client disconnects" {
        val world = World(config, simulator, boundSimulator)
        val c1 = world.createClient(GameClientInfo(name = "Client 1"))
        val c2 = world.createClient(GameClientInfo(name = "Client 2"))
        val c3 = world.createClient(GameClientInfo(name = "Client 3"))

        c1.id shouldBe 1
        c2.id shouldBe 2
        c3.id shouldBe 3

        // stale id is freed and handed out again
        world.removeClient(c1.id)
        world.createClient(GameClientInfo(name = "Replacement")).id shouldBe 1

        // a still-connected id is skipped, not reused
        world.removeClient(c2.id)
        val c4 = world.createClient(GameClientInfo(name = "Client 4"))
        c4.id shouldBe 2
        world.getClient(3)?.name shouldBe "Client 3"
    }

    "client ids wrap past 255 without colliding with connected clients" {
        val world = World(config, simulator, boundSimulator)
        repeat(254) { world.createClient(GameClientInfo(name = "Filler $it")) }
        val longLived = world.createClient(GameClientInfo(name = "Long lived"))
        longLived.id shouldBe 255

        val overflow = shouldThrow<IllegalStateException> {
            world.createClient(GameClientInfo(name = "One too many"))
        }
    }

    "bounded world never reports a body centre outside the world bounds" {
        val world = WorldFactory.create(worldConfig(enableWrapping = false))
        val client = world.createClient(GameClientInfo(name = "Client 1"))

        // body resting against the bottom wall, like BoundedWorldSimulator.edges leaves it
        val body = Body(
            id = 1,
            position = Vector2f(20f, 24f - 2f - 0.2f), // radius 2, EDGE_DELTA 0.2
            velocity = Vector2f(0f, -1f),
            mass = 1f,
            radius = 2f,
            shapeId = 2,
        )
        world.currentSimulator.addBodies(listOf(body))
        (world.currentSimulator as BaseBodySimulator).drainAdds()

        val visible = world.findVisibleShapesByClient()[client.id].orEmpty()
        visible shouldContainExactly setOf(
            VisibleShape(shapeId = 2, position = Vector2f(20f, 21.8f), bodyId = 1)
        )
    }

    "visible shapes preserve fractional world positions for smooth client scaling" {
        val world = WorldFactory.create(worldConfig(enableWrapping = false))
        world.createClient(GameClientInfo(name = "Client 1"))

        val body = Body(
            id = 1,
            position = Vector2f(15.187f, 3.713f),
            velocity = Vector2f(0f, 0f),
            mass = 1f,
            radius = 1f,
            shapeId = 2,
        )
        world.currentSimulator.addBodies(listOf(body))
        (world.currentSimulator as BaseBodySimulator).drainAdds()

        val visible = world.findVisibleShapesByClient().values.single().single()
        // must NOT be quantised to integer world units before client scaling
        visible.position.x shouldBe 15.187f
        visible.position.y shouldBe 3.713f
    }

    "wrapping world reports a body near the seam at both edges with fractional precision" {
        val world = WorldFactory.create(worldConfig(enableWrapping = true))
        val client = world.createClient(GameClientInfo(name = "Client 1"))

        // body straddling the vertical seam: centre at x=39.6, radius 1, so it pokes into x=0 side too
        val body = Body(
            id = 1,
            position = Vector2f(39.6f, 12.25f),
            velocity = Vector2f(0f, 0f),
            mass = 1f,
            radius = 1f,
            shapeId = 2,
        )
        world.currentSimulator.addBodies(listOf(body))
        (world.currentSimulator as BaseBodySimulator).drainAdds()

        val visible = world.findVisibleShapesByClient()[client.id].orEmpty()
        // wrapped copy x = 39.6f - 40f (computed identically to the implementation for float exactness)
        visible.map { it.position.x }.toSet() shouldBe setOf(39.6f, 39.6f - 40f)
        visible.forEach { it.position.y shouldBe 12.25f }
    }

    "wrapping world gives both clients the partially visible copy of a body straddling their shared edge" {
        val world = WorldFactory.create(worldConfig(locationPattern = "right", enableWrapping = true))
        val left = world.createClient(GameClientInfo(name = "Left"))
        val right = world.createClient(
            GameClientInfo(
                name = "Right",
                screenSize = ScreenSize(40, 24),
                worldSize = ScreenSize(40, 24),
            ),
        )
        // left client owns region [0,40), right owns [40,80); a body sits across the boundary at x=40.
        // The simulator wraps over the full 80-wide tiled world, so this is an interior boundary:
        // both clients receive the same absolute position and asBinary translates it per region.
        val body = Body(
            id = 1,
            position = Vector2f(40.5f, 10.125f),
            velocity = Vector2f(0f, 0f),
            mass = 1f,
            radius = 1f,
            shapeId = 2,
        )
        world.currentSimulator.addBodies(listOf(body))
        (world.currentSimulator as BaseBodySimulator).drainAdds()

        val leftVisible = world.findVisibleShapesByClient()[left.id].orEmpty()
        val rightVisible = world.findVisibleShapesByClient()[right.id].orEmpty()

        leftVisible.single().position shouldBe Vector2f(40.5f, 10.125f)
        rightVisible.single().position shouldBe Vector2f(40.5f, 10.125f)
    }

    "spawning chips away at net drift in a wrapping world" {
        val world = WorldFactory.create(worldConfig(enableWrapping = true))

        fun netMomentum(): Float {
            var px = 0f
            var py = 0f
            world.currentSimulator.withBodiesRead { bodies ->
                bodies.forEach { b ->
                    px += b.mass * b.velocity.x
                    py += b.mass * b.velocity.y
                }
            }
            return kotlin.math.sqrt(px * px + py * py)
        }

        // seed a large net drift well above the anti-drift threshold
        world.currentSimulator.addBodies(
            listOf(
                Body(id = 1, position = Vector2f(20f, 12f), velocity = Vector2f(10f, 0f), mass = 1f, radius = 1f, shapeId = 1),
                Body(id = 2, position = Vector2f(30f, 12f), velocity = Vector2f(10f, 0f), mass = 1f, radius = 1f, shapeId = 1),
            )
        )
        (world.currentSimulator as BaseBodySimulator).drainAdds()

        // each spawn opposes the remaining drift with fraction [0.5, 1] of the exact cancellation,
        // so the residual never exceeds half of what it was
        repeat(3) {
            val before = netMomentum()
            before shouldBeGreaterThan 0f
            world.addRandomBodyWithSize(2)
            (world.currentSimulator as BaseBodySimulator).drainAdds()
            netMomentum() shouldBeLessThanOrEqualTo before * 0.5f + 1e-4f
        }
    }

    "spinning spawns oppose net angular momentum" {
        val world = WorldFactory.create(worldConfig(enableWrapping = true))

        fun netAngularMomentum(): Float {
            var l = 0f
            world.currentSimulator.withBodiesRead { bodies ->
                bodies.forEach { b -> l += 0.5f * b.mass * b.radius * b.radius * b.angularVelocity }
            }
            return l
        }

        // seed a strong clockwise bias well above the anti-drift threshold
        val seeder = Body(id = 1, position = Vector2f(20f, 12f), velocity = Vector2f(0f, 0f), mass = 2f, radius = 2f, shapeId = 1)
        seeder.angularVelocity = 8f
        world.currentSimulator.addBodies(listOf(seeder))
        (world.currentSimulator as BaseBodySimulator).drainAdds()

        repeat(3) {
            val before = netAngularMomentum()
            before shouldBeGreaterThan 0f
            world.addRandomBodyWithSize(3)
            (world.currentSimulator as BaseBodySimulator).drainAdds()
            kotlin.math.abs(netAngularMomentum()) shouldBeLessThanOrEqualTo kotlin.math.abs(before) * 0.5f + 1e-4f
        }
    }
})

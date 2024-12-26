package domain

import config.WorldConfig
import geometry.Point
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import simulator.WorldSimulator

internal class WorldTest {
    private val simulator: WorldSimulator = mockk(relaxed = true)
    private val boundSimulator: WorldSimulator = mockk(relaxed = true)
    private val config: WorldConfig = WorldConfig().also { it.shouldAutoStart = false }

    @BeforeEach
    fun setUp() {

    }

    @Test
    fun `can add clients`() {
        val world = World(config, simulator, boundSimulator)
        val c1 = world.createClient(GameClientInfo(name = "Client 1"))
        val c2 = world.createClient(GameClientInfo(name = "Client 2"))

        assertThat(world.getClient(c1.id)?.id).isEqualTo(c1.id)
        assertThat(world.getClient(c1.id)?.name).isEqualTo("Client 1")
        assertThat(world.getClient(c1.id)?.position).isEqualTo(Point(0,0))

        assertThat(world.getClient(c2.id)?.id).isEqualTo(c2.id)
        assertThat(world.getClient(c2.id)?.name).isEqualTo("Client 2")
        assertThat(world.getClient(c2.id)?.position).isEqualTo(Point(1,0))

        assertThat(world.getClient(69)).isNull()

        assertThat(world.at(Point(0,0))?.id).isEqualTo(c1.id)
        assertThat(world.at(Point(1,0))?.id).isEqualTo(c2.id)

        assertThat(world.at(Point(5,5))).isNull()

    }

    @Test
    fun `should allow removing client and putting new client in vacated position`() {
        val world = World(config, simulator, boundSimulator)
        world.createClient(GameClientInfo(name = "Client 1"))
        val c2 = world.createClient(GameClientInfo(name = "Client 2"))
        world.createClient(GameClientInfo(name = "Client 3"))

        // remove client 2, thus freeing up the 1,0 slot
        world.removeClient(c2.id)
        assertThat(world.getClient(c2.id)).isNull()
        assertThat(world.at(Point(1,0))).isNull()

        // add a new client and ensure it was in the free slot at 1,0
        val c4 = world.createClient(GameClientInfo(name = "Client 4"))
        assertThat(world.getClient(c4.id)!!.position).isEqualTo(Point(1,0))
    }

    @Test
    fun `boundary size stretches to maximum rectangle to contain all clients and is 1 based`() {
        val world = World(config, simulator, boundSimulator)
        world.createClient(GameClientInfo(name = "Client 1"))
        assertThat(world.worldBoundary()).isEqualTo(Point(1,1))
        world.createClient(GameClientInfo(name = "Client 2"))
        assertThat(world.worldBoundary()).isEqualTo(Point(2,1))
        world.createClient(GameClientInfo(name = "Client 3"))
        assertThat(world.worldBoundary()).isEqualTo(Point(2,2))
        world.createClient(GameClientInfo(name = "Client 4"))
        assertThat(world.worldBoundary()).isEqualTo(Point(2,2))
        world.createClient(GameClientInfo(name = "Client 5"))
        assertThat(world.worldBoundary()).isEqualTo(Point(3,2))
        world.createClient(GameClientInfo(name = "Client 6"))
        assertThat(world.worldBoundary()).isEqualTo(Point(3,2))
        world.createClient(GameClientInfo(name = "Client 7"))
        assertThat(world.worldBoundary()).isEqualTo(Point(3,3))
        world.createClient(GameClientInfo(name = "Client 8"))
        assertThat(world.worldBoundary()).isEqualTo(Point(3,3))
        world.createClient(GameClientInfo(name = "Client 9"))
        assertThat(world.worldBoundary()).isEqualTo(Point(3,3))
    }

    @Test
    fun `world boundary size with no clients has size 1,1`() {
        val world = World(config, simulator, boundSimulator)
        assertThat(world.worldBoundary()).isEqualTo(Point(1,1))
    }

    /*
    @Test
    fun `create visible shapes by client for 2 clients and single wrapping in horizontal direction`() {
        val clients = listOf(
            GameClient(id = 1, name = "gc1", position = Point(0, 0)),
            GameClient(id = 2, name = "gc2", position = Point(1, 0))
        ) // .map { it.apply { it.updateWorldBounds(50, 50) } }

        val config = WorldConfig().also { it.width = 100; it.height = 50; it.scalingFactor = 1 }
        val simulator = WorldSimulator(config)
        val world = World(config, simulator, boundSimulator)
        clients.forEach { world.addClient(it) }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val bodyA = Body.from(position = Vector2f(1f, 10f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        simulator.addBodies(listOf(bodyA))

        val visibleShapesByClient = world.findVisibleShapesByClient()
        assertThat(visibleShapesByClient[1]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(1, 10)),      // body position without reflection
        )
        assertThat(visibleShapesByClient[2]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(101, 10)),     // body wrapped on the left in client 2
        )
    }

    @Test
    fun `create visible shapes by client for 2 clients and single wrapping with wrapping in vertical direction`() {
        val clients = listOf(
            GameClient(id = 1, name = "gc1", position = Point(0, 0)),
            GameClient(id = 2, name = "gc2", position = Point(0, 1))
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val simulator = WorldSimulator(config)
        val world = World(config, simulator, boundSimulator)
        clients.forEach { world.addClient(it) }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val bodyA = Body.from(position = Vector2f(10f, 1f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        simulator.addBodies(listOf(bodyA))

        val visibleShapesByClient = world.findVisibleShapesByClient()
        assertThat(visibleShapesByClient[1]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(10, 1)),
        )
        assertThat(visibleShapesByClient[2]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(10, 101)),
        )
    }

    @Test
    fun `create visible shapes by client for 4 clients in all 4 directions from client 1 position`() {
        val clients = listOf(
            GameClient(id = 1, name = "gc1", position = Point(0, 0)),
            GameClient(id = 2, name = "gc2", position = Point(1, 0)),
            GameClient(id = 3, name = "gc3", position = Point(1, 1)),
            GameClient(id = 4, name = "gc4", position = Point(0, 1)),
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val simulator = WorldSimulator(config)
        val world = World(config, simulator, boundSimulator)
        clients.forEach { world.addClient(it) }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val bodyA = Body.from(position = Vector2f(1f, 1f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        simulator.addBodies(listOf(bodyA))

        val visibleShapesByClient = world.findVisibleShapesByClient()
        assertThat(visibleShapesByClient[1]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(1, 1)),
        )
        assertThat(visibleShapesByClient[2]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(101, 1)),
        )
        assertThat(visibleShapesByClient[3]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(101, 101)),
        )
        assertThat(visibleShapesByClient[4]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(1, 101)),
        )
    }

    @Test
    fun `create visible shapes by client for 4 clients in all 4 directions from client 3 position`() {
        val clients = listOf(
            GameClient(id = 1, name = "gc1", position = Point(0, 0)),
            GameClient(id = 2, name = "gc2", position = Point(1, 0)),
            GameClient(id = 3, name = "gc3", position = Point(1, 1)),
            GameClient(id = 4, name = "gc4", position = Point(0, 1)),
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val simulator = WorldSimulator(config)
        val world = World(config, simulator, boundSimulator)
        clients.forEach { world.addClient(it) }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val bodyA = Body.from(position = Vector2f(51f, 51f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        simulator.addBodies(listOf(bodyA))

        val visibleShapesByClient = world.findVisibleShapesByClient()
        assertThat(visibleShapesByClient[1]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(51, 51)),
        )
        assertThat(visibleShapesByClient[2]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(51, 51)),
        )
        assertThat(visibleShapesByClient[3]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(51, 51)),
        )
        assertThat(visibleShapesByClient[4]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(51, 51)),
        )
    }

    @Test
    fun `create visible shapes by client for 4 clients where only contained in client 2`() {
        val clients = listOf(
            GameClient(id = 1, name = "gc1", position = Point(0, 0)),
            GameClient(id = 2, name = "gc2", position = Point(1, 0)),
            GameClient(id = 3, name = "gc3", position = Point(1, 1)),
            GameClient(id = 4, name = "gc4", position = Point(0, 1)),
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val simulator = WorldSimulator(config)
        val world = World(config, simulator, boundSimulator)
        clients.forEach { world.addClient(it) }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val bodyA = Body.from(position = Vector2f(61f, 11f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        simulator.addBodies(listOf(bodyA))

        val visibleShapesByClient = world.findVisibleShapesByClient()
        assertThat(visibleShapesByClient[1]).isEmpty()
        assertThat(visibleShapesByClient[2]).containsExactlyInAnyOrder(
            VisibleShape(0, Point(61, 11)),
        )
        assertThat(visibleShapesByClient[3]).isEmpty()
        assertThat(visibleShapesByClient[4]).isEmpty()
    }

    @Test
    fun `create visible shapes by client for 4 clients with multiple bodies from previous tests`() {
        val clients = listOf(
            GameClient(id = 1, name = "gc1", position = Point(0, 0)),
            GameClient(id = 2, name = "gc2", position = Point(1, 0)),
            GameClient(id = 3, name = "gc3", position = Point(1, 1)),
            GameClient(id = 4, name = "gc4", position = Point(0, 1)),
        ).map { it.apply { it.updateWorldBounds(50, 50) } }

        val simulator = WorldSimulator(config)
        val world = World(config, simulator, boundSimulator)
        clients.forEach { world.addClient(it) }

        val shapes = mutableMapOf(
            0 to Shape(0, 1.0f, 5, emptyList()),
        )
        val bodyA = Body.from(position = Vector2f(1f, 1f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        val bodyB = Body.from(position = Vector2f(51f, 51f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        val bodyC = Body.from(position = Vector2f(61f, 11f), velocity = Vector2f(0f, 0f), shape = shapes[0]!!)
        simulator.addBodies(listOf(bodyA, bodyB, bodyC))

        val visibleShapesByClient = world.findVisibleShapesByClient()
        assertThat(visibleShapesByClient[1]).containsExactlyInAnyOrder(
            VisibleShape(shapeId=0, position=Point(x=1, y=1)),
            VisibleShape(shapeId=0, position=Point(x=51, y=51)),
        )
        assertThat(visibleShapesByClient[2]).containsExactlyInAnyOrder(
            VisibleShape(shapeId=0, position=Point(x=101, y=1)),
            VisibleShape(shapeId=0, position=Point(x=51, y=51)),
            VisibleShape(shapeId=0, position=Point(x=61, y=11)),
        )
        assertThat(visibleShapesByClient[3]).containsExactlyInAnyOrder(
            VisibleShape(shapeId=0, position=Point(x=101, y=101)),
            VisibleShape(shapeId=0, position=Point(x=51, y=51)),
        )
        assertThat(visibleShapesByClient[4]).containsExactlyInAnyOrder(
            VisibleShape(shapeId=0, position=Point(x=1, y=101)),
            VisibleShape(shapeId=0, position=Point(x=51, y=51)),
        )
    }
     */
}
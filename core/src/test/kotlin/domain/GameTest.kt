package domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import items.GameItem
import org.joml.Vector2f
import simulator.GameSimulator

fun createGameItem(id: Int, position: Vector2f = Vector2f(5f, 5f), velocity: Vector2f = Vector2f(1f, 0f), radius: Float = 1f, mass: Float = 1f): GameItem {
    return object : GameItem {
        override val id = id
        override val position = position
        override val velocity = velocity
        override val radius = radius
        override var direction = Math.PI / 2.0
        override val mass = mass
    }
}

fun createGameClient(id: Int, name: String, width: Int, height: Int): GameClientNew {
    return object : GameClientNew {
        override val id = id
        override val name = name
        override val version = 1
        override val screenWidth = width
        override val screenHeight = height
    }
}

class GameTest : StringSpec({
    "can see visible items within range" {
        val client1 = createGameClient(1, "c1", 4, 2)
        val client2 = createGameClient(2, "c2", 9, 4)
        val client3 = createGameClient(3, "c3", 7, 6)
        val client4 = createGameClient(4, "c4", 5, 4)
        val body1 = createGameItem(id = 1, position = Vector2f(5f, 3f))
        val body2 = createGameItem(id = 2, position = Vector2f(10f, 3f))
        val body3 = createGameItem(id = 3, position = Vector2f(15f, 3f))
        val body4 = createGameItem(id = 4, position = Vector2f(15f, 8f))
        val items = mutableListOf(body1, body2, body3, body4)
        val simulator = mockk<GameSimulator>(relaxed = true)
        every { simulator.width } returns 20
        every { simulator.height } returns 10
        every { simulator.items } returns items

        // emulate the add item into our own list to capture it
        val item = slot<GameItem>()
        every { simulator.addItem(capture(item)) } answers { items.add(item.captured) }

        val game = Game(simulator)
        val p1 = game.addClient(client1, Vector2f(5f, 3f))
        val p2 = game.addClient(client2, Vector2f(6.5f, 3f))
        val p3 = game.addClient(client3, Vector2f(12.5f, 4f))
        val p4 = game.addClient(client4, Vector2f(14.5f, 7.5f))

        // change the client to 45 degree angle
        p4.direction = Math.PI / 4.0

        val vis = game.determineVisibleItems(false)

        vis.size shouldBe 4 // Ensure we have visibility information for exactly 4 players
        vis[p1.id]?.map { it.id } shouldContainExactlyInAnyOrder listOf(1, 6) // Player 1 can see body1 and player2
        vis[p2.id]?.map { it.id } shouldContainExactlyInAnyOrder listOf(1, 2, 5) // Player 2 can see body1, body2, and player1
        vis[p3.id]?.map { it.id } shouldContainExactlyInAnyOrder listOf(2, 3) // Player 3 can see body2 and body3
        vis[p4.id]?.map { it.id } shouldContainExactlyInAnyOrder listOf(4) // Player 4 can see body4
    }

    "item visibility check player top left" {
        val client1 = createGameClient(1, "c1", 5, 5)
        val body1 = createGameItem(id = 1, position = Vector2f(19f, 19f))
        val body2 = createGameItem(id = 2, position = Vector2f(10f, 10f))
        val items = mutableListOf(body1, body2)

        val simulator = mockk<GameSimulator>(relaxed = true)
        every { simulator.width } returns 20
        every { simulator.height } returns 20
        every { simulator.items } returns items
        val item = slot<GameItem>()
        every { simulator.addItem(capture(item)) } answers { items.add(item.captured) }
        val game = Game(simulator)
        // player at 1,1 should see point at 19,19 with screen size 5,5, but not at point 10,10
        val p1 = game.addClient(client1, Vector2f(1f, 1f))

        // with wrapping
        val vis1 = game.determineVisibleItems(true)
        vis1.size shouldBe 1
        vis1[p1.id]?.map { it.id } shouldContainExactlyInAnyOrder listOf(1)

        // no wrapping
        val vis2 = game.determineVisibleItems(false)
        vis2.size shouldBe 1
        vis2[p1.id]?.shouldHaveSize(0)
    }

    "item visibility check player bottom left" {
        val client1 = createGameClient(1, "c1", 5, 5)
        val body1 = createGameItem(id = 1, position = Vector2f(19f, 1f))
        val body2 = createGameItem(id = 2, position = Vector2f(10f, 10f))
        val items = mutableListOf(body1, body2)

        val simulator = mockk<GameSimulator>(relaxed = true)
        every { simulator.width } returns 20
        every { simulator.height } returns 20
        every { simulator.items } returns items
        val item = slot<GameItem>()
        every { simulator.addItem(capture(item)) } answers { items.add(item.captured) }
        val game = Game(simulator)
        // player at 1,19 should see point at 19,1 with screen size 5,5, but not at point 10,10
        val p1 = game.addClient(client1, Vector2f(1f, 19f))

        val vis1 = game.determineVisibleItems(true)
        vis1.size shouldBe 1
        vis1[p1.id]?.map { it.id } shouldContainExactlyInAnyOrder listOf(1)

        val vis2 = game.determineVisibleItems(false)
        vis2.size shouldBe 1
        vis2[p1.id]?.shouldHaveSize(0)
    }

    "item visibility check player top right" {
        val client1 = createGameClient(1, "c1", 5, 5)
        val body1 = createGameItem(id = 1, position = Vector2f(1f, 19f))
        val body2 = createGameItem(id = 2, position = Vector2f(10f, 10f))
        val items = mutableListOf(body1, body2)

        val simulator = mockk<GameSimulator>(relaxed = true)
        every { simulator.width } returns 20
        every { simulator.height } returns 20
        every { simulator.items } returns items
        val item = slot<GameItem>()
        every { simulator.addItem(capture(item)) } answers { items.add(item.captured) }
        val game = Game(simulator)
        // player at 19,1 should see point at 1,19 with screen size 5,5, but not at point 10,10
        val p1 = game.addClient(client1, Vector2f(19f, 1f))

        val vis1 = game.determineVisibleItems(true)
        vis1.size shouldBe 1
        vis1[p1.id]?.map { it.id } shouldContainExactlyInAnyOrder listOf(1)

        val vis2 = game.determineVisibleItems(false)
        vis2.size shouldBe 1
        vis2[p1.id]?.shouldHaveSize(0)
    }

    "item visibility check player bottom right" {
        val client1 = createGameClient(1, "c1", 5, 5)
        val body1 = createGameItem(id = 1, position = Vector2f(1f, 1f))
        val body2 = createGameItem(id = 2, position = Vector2f(10f, 10f))
        val items = mutableListOf(body1, body2)

        val simulator = mockk<GameSimulator>(relaxed = true)
        every { simulator.width } returns 20
        every { simulator.height } returns 20
        every { simulator.items } returns items
        val item = slot<GameItem>()
        every { simulator.addItem(capture(item)) } answers { items.add(item.captured) }
        val game = Game(simulator)
        // player at 19,19 should see point at 1,1 with screen size 5,5, but not at point 10,10
        val p1 = game.addClient(client1, Vector2f(19f, 19f))

        val vis1 = game.determineVisibleItems(true)
        vis1.size shouldBe 1
        vis1[p1.id]?.map { it.id } shouldContainExactlyInAnyOrder listOf(1)

        val vis2 = game.determineVisibleItems(false)
        vis2.size shouldBe 1
        vis2[p1.id]?.shouldHaveSize(0)
    }

})

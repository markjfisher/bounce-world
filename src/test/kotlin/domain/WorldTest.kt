package domain

import geometry.Point
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class WorldTest {
    @Test
    fun `can add clients`() {
        val world = World()
        val c1 = GameClient(id = "1", name = "Client 1")
        val c2 = GameClient(id = "2", name = "Client 2")
        world.addClient(c1)
        world.addClient(c2)

        assertThat(world.getClient("1")?.id).isEqualTo("1")
        assertThat(world.getClient("1")?.name).isEqualTo("Client 1")
        assertThat(world.getClient("1")?.position).isEqualTo(Point(0,0))

        assertThat(world.getClient("2")?.id).isEqualTo("2")
        assertThat(world.getClient("2")?.name).isEqualTo("Client 2")
        assertThat(world.getClient("2")?.position).isEqualTo(Point(1,0))

        assertThat(world.getClient("foo")).isNull()

        assertThat(world.at(Point(0,0))?.id).isEqualTo(c1.id)
        assertThat(world.at(Point(1,0))?.id).isEqualTo(c2.id)

        assertThat(world.at(Point(5,5))).isNull()

    }

    @Test
    fun `should allow removing client and putting new client in vacated position`() {
        val world = World()
        val c1 = GameClient(id = "1", name = "Client 1")
        val c2 = GameClient(id = "2", name = "Client 2")
        val c3 = GameClient(id = "3", name = "Client 1")
        world.addClient(c1) // 0,0
        world.addClient(c2) // 1,0
        world.addClient(c3) // 1,1

        // remove client 2, thus freeing up the 1,0 slot
        world.removeClient("2")
        assertThat(world.getClient("2")).isNull()
        assertThat(world.at(Point(1,0))).isNull()

        // add a new client and ensure it was in the free slot at 1,0
        val c4 = GameClient(id = "4", name = "Client 4")
        world.addClient(c4)
        assertThat(world.getClient("4")?.position).isEqualTo(Point(1,0))
    }

    @Test
    fun `boundary size stretches to maximum rectangle to contain all clients and is 1 based`() {
        val world = World()
        world.addClient(GameClient(id = "1", name = "Client 1"))
        assertThat(world.worldBoundary()).isEqualTo(Point(1,1))
        world.addClient(GameClient(id = "2", name = "Client 2"))
        assertThat(world.worldBoundary()).isEqualTo(Point(2,1))
        world.addClient(GameClient(id = "3", name = "Client 3"))
        assertThat(world.worldBoundary()).isEqualTo(Point(2,2))
        world.addClient(GameClient(id = "4", name = "Client 4"))
        assertThat(world.worldBoundary()).isEqualTo(Point(2,2))
        world.addClient(GameClient(id = "5", name = "Client 5"))
        assertThat(world.worldBoundary()).isEqualTo(Point(3,2))
        world.addClient(GameClient(id = "6", name = "Client 6"))
        assertThat(world.worldBoundary()).isEqualTo(Point(3,2))
        world.addClient(GameClient(id = "7", name = "Client 7"))
        assertThat(world.worldBoundary()).isEqualTo(Point(3,3))
        world.addClient(GameClient(id = "8", name = "Client 8"))
        assertThat(world.worldBoundary()).isEqualTo(Point(3,3))
        world.addClient(GameClient(id = "9", name = "Client 9"))
        assertThat(world.worldBoundary()).isEqualTo(Point(3,3))
    }

    @Test
    fun `world boundary size with no points has size 0,0`() {
        val world = World()
        assertThat(world.worldBoundary()).isEqualTo(Point(0,0))
    }

}
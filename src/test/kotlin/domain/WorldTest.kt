package domain

import config.WorldConfiguration
import geometry.Point
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class WorldTest {
    @Test
    fun `can add clients`() {
        val world = World(WorldConfiguration())
        val c1 = world.createClient(GameClientInfo(name = "Client 1"))
        val c2 = world.createClient(GameClientInfo(name = "Client 2"))

        assertThat(world.getClient(c1.id)?.id).isEqualTo(c1.id)
        assertThat(world.getClient(c1.id)?.name).isEqualTo("Client 1")
        assertThat(world.getClient(c1.id)?.position).isEqualTo(Point(0,0))

        assertThat(world.getClient(c2.id)?.id).isEqualTo(c2.id)
        assertThat(world.getClient(c2.id)?.name).isEqualTo("Client 2")
        assertThat(world.getClient(c2.id)?.position).isEqualTo(Point(1,0))

        assertThat(world.getClient("foo")).isNull()

        assertThat(world.at(Point(0,0))?.id).isEqualTo(c1.id)
        assertThat(world.at(Point(1,0))?.id).isEqualTo(c2.id)

        assertThat(world.at(Point(5,5))).isNull()

    }

    @Test
    fun `should allow removing client and putting new client in vacated position`() {
        val world = World(WorldConfiguration())
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
        val world = World(WorldConfiguration())
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
        val world = World(WorldConfiguration())
        assertThat(world.worldBoundary()).isEqualTo(Point(1,1))
    }

}
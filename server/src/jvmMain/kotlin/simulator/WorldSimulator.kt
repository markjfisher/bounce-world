package simulator

import domain.Body
import domain.BodyView

interface WorldSimulator {
    var width: Int
    var height: Int
    val currentStep: Int

    fun step()
    fun addBodies(bodies: List<Body>)
    fun reset()

    // safe read helpers
    /** Execute a read under the simulator’s lock on a stable snapshot view (no mutation allowed). */
    fun <T> withBodiesRead(block: (List<Body>) -> T): T

    fun forEachBody(block: (Body) -> Unit) {
        withBodiesRead { list -> list.forEach(block) }
    }

    fun <R> mapBodies(block: (Body) -> R): List<R> =
        withBodiesRead { list -> list.map(block) }

    fun <K> groupingBodiesBy(key: (Body) -> K): Grouping<Body, K> =
        withBodiesRead { list -> list.groupingBy(key) }

    /** Fast count under lock. */
    fun bodyCount(): Int

    /** Immutable DTO snapshot for API/serialization. */
    fun snapshotBodyViews(): List<BodyView>

    fun collisionsCopy(): Set<Int>

    fun nextBodyId(): Int
}

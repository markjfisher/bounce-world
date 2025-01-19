package simulator

import domain.GameEvent
import items.GameItem

interface GameSimulator {
    // The simulation can potentially grow as needed
    var width: Int
    var height: Int

    // a value to indicate what step is currently happening. It may be rolled over by the Game.
    var currentStep: Int

    // what happened in current step
    val events: MutableSet<GameEvent>

    // the items in the simulation
    val items: MutableList<GameItem>
    fun addItem(item: GameItem)

    // a way to move the simulation on
    fun step()

    // a way to clear the simulator
    fun reset()
}

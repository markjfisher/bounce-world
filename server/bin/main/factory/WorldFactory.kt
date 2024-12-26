package factory

import config.WorldConfig
import domain.World
import simulator.BoundedWorldSimulator
import simulator.WrappingWorldSimulator

object WorldFactory {
    fun create(config: WorldConfig): World {
        val wrappedSimulator = WrappingWorldSimulator(config)
        val boundedSimulator = BoundedWorldSimulator(config)
        return World(config, wrappedSimulator, boundedSimulator)
    }
}
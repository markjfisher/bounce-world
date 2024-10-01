package domain

import config.WorldConfiguration
import simulator.WrappedBodySimulator
import simulator.BoundedBodySimulator

object WorldFactory {
    fun create(config: WorldConfiguration): World {
        val wrappedSimulator = WrappedBodySimulator(
            width = config.width.toFloat(),
            height = config.height.toFloat(),
            scalingFactor = config.scalingFactor
        )
        val boundedSimulator = BoundedBodySimulator(
            width = config.width.toFloat(),
            height = config.height.toFloat(),
            scalingFactor = config.scalingFactor
        )
        return World(config, wrappedSimulator, boundedSimulator)
    }
}
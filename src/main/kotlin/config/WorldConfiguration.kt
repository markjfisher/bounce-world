package config

import domain.World.Companion.SCREEN_HEIGHT
import domain.World.Companion.SCREEN_WIDTH
import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("world")
class WorldConfiguration {
    var width: Int = SCREEN_WIDTH
    var height: Int = SCREEN_HEIGHT
    var scalingFactor: Int = 4
    var updatesPerSecond: Int = 5
    var shouldAutoStart: Boolean = true
    var initialSpeed: Float = 1.5f
    var heartbeatTimeoutMillis: Long = 10000
    var locationPattern: String = "grid"
    var enableWrapping: Boolean = true
}
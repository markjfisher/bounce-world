package config

import io.ktor.server.config.*

class WorldConfig(config: ApplicationConfig) {
    var width = config.property("world.width").getString().toInt()
    var height = config.property("world.height").getString().toInt()
    var scalingFactor = config.property("world.scalingFactor").getString().toInt()
    var updatesPerSecond = config.property("world.updatesPerSecond").getString().toInt()
    var shouldAutoStart = config.property("world.shouldAutoStart").getString().toBoolean()
    var initialSpeed: Float = config.property("world.initialSpeed").getString().toFloat()
    var heartbeatTimeoutMillis = config.property("world.heartbeatTimeoutMillis").getString().toLong()
    var locationPattern = config.property("world.locationPattern").getString()
    var enableWrapping = config.property("world.enableWrapping").getString().toBoolean()

    var tcpHost = config.property("world.tcp.host").getString()
    var tcpPort = config.property("world.tcp.port").getString().toInt()

}

package config

import io.ktor.server.config.*

class WorldConfig(config: ApplicationConfig) {
    val width = config.property("world.width").getString().toInt()
    val height = config.property("world.height").getString().toInt()
    val scalingFactor = config.property("world.scalingFactor").getString().toInt()
    val updatesPerSecond = config.property("world.updatesPerSecond").getString().toInt()
    val shouldAutoStart = config.property("world.shouldAutoStart").getString().toBoolean()
    val initialSpeed: Float = config.property("world.initialSpeed").getString().toFloat()
    val heartbeatTimeoutMillis = config.property("world.heartbeatTimeoutMillis").getString().toLong()
    val locationPattern = config.property("world.locationPattern").getString()
    val enableWrapping = config.property("world.enableWrapping").getString().toBoolean()

    val tcpHost = config.property("world.tcp.host").getString()
    val tcpPort = config.property("world.tcp.port").getString().toInt()

}

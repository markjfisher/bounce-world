package config

data class WorldConfig(
    var width: Int,
    var height: Int,
    var updatesPerSecond: Int,
    var shouldAutoStart: Boolean,
    var initialSpeed: Float,
    var heartbeatTimeoutMillis: Long,
    var locationPattern: String,
    var enableWrapping: Boolean,
    var tcpHost: String,
    var tcpPort: Int,
)

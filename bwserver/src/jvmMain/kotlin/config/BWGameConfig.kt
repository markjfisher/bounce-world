package config

data class BWGameConfig(
    override var width: Int,
    override var height: Int,
    override var updatesPerSecond: Int,
    override var autoStart: Boolean,
    override var heartbeatTimeoutMillis: Long,
): GameConfig {
    var initialSpeed: Float = 8f
    var locationPattern: String = "grid"
    var tcpHost: String = "localhost"
    var tcpPort: Int = 9002
}

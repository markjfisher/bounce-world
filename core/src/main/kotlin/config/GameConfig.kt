package config

interface GameConfig {
    var width: Int
    var height: Int
    var updatesPerSecond: Int
    var autoStart: Boolean
    var heartbeatTimeoutMillis: Long
}
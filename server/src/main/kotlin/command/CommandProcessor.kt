package command

import config.WorldConfig

class CommandProcessor(private val worldConfig: WorldConfig) {
    fun process(command: String): ByteArray {
        return when {
            command.startsWith("w/") -> getWorldData(command.substringAfter("w/").toInt())
            command == "ws" -> getWorldState()
            command == "status" -> getState()
            command == "freeze" -> toggleFreeze()
            command.startsWith("add/") -> addBody(command.substringAfter("add/").toInt())
            command == "reset" -> resetWorld()
            command == "inc" -> increaseSpeed()
            command == "dec" -> decreaseSpeed()
            command.startsWith("cmd/put/") -> {
                val parts = command.split("/")
                if (parts.size >= 4) {
                    clientCommand(parts[2], parts[3])
                } else {
                    byteArrayOf(0) // Error response
                }
            }
            command.startsWith("cmd/broadcast/") -> {
                val parts = command.split("/")
                if (parts.size >= 5) {
                    broadcastCommand(parts[2], parts[3], parts[4])
                } else {
                    byteArrayOf(0) // Error response
                }
            }
            else -> byteArrayOf(0) // Default response for unknown commands
        }
    }

    private fun getWorldData(clientId: Int): ByteArray { /* ... */ }
    private fun getWorldState(): ByteArray { /* ... */ }
    private fun getState(): ByteArray { /* ... */ }
    private fun toggleFreeze(): ByteArray { /* ... */ }
    private fun addBody(size: Int): ByteArray { /* ... */ }
    private fun resetWorld(): ByteArray { /* ... */ }
    private fun increaseSpeed(): ByteArray { /* ... */ }
    private fun decreaseSpeed(): ByteArray { /* ... */ }
    private fun clientCommand(clientId: String, cmd: String): ByteArray { /* ... */ }
    private fun broadcastCommand(clientId: String, time: String, message: String): ByteArray { /* ... */ }
}
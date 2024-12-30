package command

import domain.World

class CommandProcessor(private val world: World) {
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

    private fun getWorldData(clientId: Int): ByteArray {
        // Implement using world instance
        return byteArrayOf() // Placeholder
    }

    private fun getWorldState(): ByteArray {
        // Implement using world instance
        return byteArrayOf() // Placeholder
    }

    private fun getState(): ByteArray {
        // Implement using world instance
        println("doing status")
        val response = "status called".toByteArray(Charsets.UTF_8)
        return response
    }

    private fun toggleFreeze(): ByteArray {
        world.toggleFrozen()
        return byteArrayOf(1) // Success response
    }

    private fun addBody(size: Int): ByteArray {
        world.addBody(size)
        return byteArrayOf(1) // Success response
    }

    private fun resetWorld(): ByteArray {
        world.resetWorld()
        return byteArrayOf(1) // Success response
    }

    private fun increaseSpeed(): ByteArray {
        world.increaseSpeed()
        return byteArrayOf(1) // Success response
    }

    private fun decreaseSpeed(): ByteArray {
        world.decreaseSpeed()
        return byteArrayOf(1) // Success response
    }

    private fun clientCommand(clientId: String, cmd: String): ByteArray {
        // Implement using world instance
        return byteArrayOf() // Placeholder
    }

    private fun broadcastCommand(clientId: String, time: String, message: String): ByteArray {
        // Implement using world instance
        return byteArrayOf() // Placeholder
    }
}
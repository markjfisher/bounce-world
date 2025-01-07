package server

import command.ClientCommandProcessor
import command.ShapesCommandProcessor
import command.WorldCommandProcessor
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logger

inline fun <reified T> serializeObjectToByteArray(obj: T): ByteArray {
    // Explicitly get the serializer for the type T and use it to serialize obj
    val jsonString = Json.encodeToString(obj)

    // Convert the JSON string to a ByteArray
    return jsonString.toByteArray(Charsets.UTF_8)
}

class TcpServer(
    private val wcp: WorldCommandProcessor,
    private val ccp: ClientCommandProcessor,
    private val scp: ShapesCommandProcessor,
    private val host: String,
    private val port: Int,
    private val scope: CoroutineScope,
) {
    fun start() {
        logger.info("Starting TCP server")
        scope.launch {
            val serverSocket = aSocket(SelectorManager(this.coroutineContext)).tcp().bind(host, port)
            while (true) {
                val socket = serverSocket.accept()
                // By launching here, a new coroutine handles this connection, so the server is immediately free to handle
                // a new connection which is how we achieve high concurrency
                launch {
                    handleClient(socket)
                }
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)
        var isKeepActive = true

        try {
            while (isKeepActive) {
                val buffer = ByteArray(1024)
                val bytesRead: Int? = withTimeoutOrNull(5_000) {
                    input.readAvailable(buffer, 0, buffer.size)
                }

                if (bytesRead == null) {
                    logger.info("No client command to process, will continue waiting.")
                } else if (bytesRead == -1 || input.isClosedForRead) {
                    // logger.warn("connection closed")
                    break
                } else {
                    val command = buffer.copyOf(bytesRead)
                    // in this scenario we can strip the string of any CR or LF (e.g. for linux cli from "echo").
                    // For a more general binary scenario we would not convert to string, but everything for Bouncy World is strings on the commands
                    val commandString = command.toString(Charsets.UTF_8).trim()

                    // Check if this is from a client maintaining a persistent connection (sending command with "x-" at the start)
                    isKeepActive = commandString.startsWith("x-")
                    val response = process(commandString.substringAfter("x-").trim())
                    output.writeByteArray(response)
                }
            }
        } catch (e: Throwable) {
            logger.error("Error while handling client", e)
        } finally {
            withContext(NonCancellable) {
                socket.close()
            }
        }
    }

    private fun process(command: String): ByteArray {
        val commandRegex = """^[a-zA-Z-]+\s""".toRegex()

        // rp == remove prefix
        fun rp(command: String): String = command.replaceFirst(commandRegex, "")

        return when {
            // WORLD commands
            command.startsWith("w ") -> doGetWorldData(rp(command))
            command == "ws" -> wcp.getWorldState()
            command == "status" -> serializeObjectToByteArray(wcp.getStatus())
            command == "reset" -> wcp.resetWorld()
            command == "inc" -> wcp.increaseSpeed()
            command == "dec" -> wcp.decreaseSpeed()
            command == "freeze" -> wcp.toggleFreeze()
            command == "msg" -> wcp.getLatestMessage().toByteArray(Charsets.UTF_8)
            command == "who" -> wcp.who().toByteArray(Charsets.UTF_8)
            command.startsWith("new-body ") -> doNewBody(rp(command))
            command.startsWith("add-body ") -> doAddBody(rp(command))
            command.startsWith("cmd-put ") -> doClientCommand(rp(command))
            command.startsWith("cmd-get ") -> doFetchCommands(rp(command))
            command.startsWith("cmd-broadcast ") -> doBroadcastCommand(rp(command))

            // CLIENT commands
            command.startsWith("add-client ") -> doAddClient(rp(command))

            // SHAPES commands
            command == "shape-data" -> scp.getShapesData()
            command == "shape-count" -> byteArrayOf(scp.getShapesCount().toByte())
            else -> {
                logger.error("Unknown command: $command")
                byteArrayOf(0)
            }
        }
    }

    private fun doGetWorldData(arg: String): ByteArray {
        val clientId = arg.toIntOrNull() ?: return "Invalid ClientID: $arg".toByteArray()
        return wcp.getWorldData(clientId)
    }

    private fun doNewBody(arg: String): ByteArray {
        val (shapeIdString, clientIdString) = arg.split(',', limit = 2)
        val shapeId = shapeIdString.toIntOrNull()
        val clientId = clientIdString.toIntOrNull()
        return if (shapeId != null && clientId != null) {
            wcp.addBody(shapeId, clientId)
        } else {
            "Invalid size parameter".toByteArray()
        }
    }

    private fun doAddBody(arg: String): ByteArray {
        val size = arg.toIntOrNull() ?: return "Invalid size parameter: $arg".toByteArray()
        return wcp.addRandomBodyWithSize(size)
    }

    private fun doClientCommand(arg: String): ByteArray {
        // cmd-put clientId,cmd  # the clientId can be "ALL" for all clients or just their id value
        val (clientId, cmd) = arg.split(',', limit = 2)
        if (clientId.isEmpty() || cmd.isEmpty()) return "Invalid Command: $arg, should be in format 'cmd-put clientId,cmd'".toByteArray()
        return wcp.clientCommand(clientId, cmd)
    }

    private fun doFetchCommands(arg: String): ByteArray {
        arg.toIntOrNull() ?: return "Invalid ClientID: $arg".toByteArray()
        return wcp.fetchCommands(arg)
    }

    private fun doBroadcastCommand(arg: String): ByteArray {
        // cmd-broadcast [clientId|ALL],time,message string
        val parts = arg.split(',', limit = 3)
        return if (parts.size == 3) {
            val (clientId, time, message) = parts
            wcp.broadcastCommand(clientId, time, message)
        } else {
            "Invalid parameters $arg".toByteArray()
        }
    }

    private fun doAddClient(arg: String): ByteArray {
        // add-client name,version,screenWidth,screenHeight
        val parts = arg.split(',', limit = 4)
        return if (parts.size == 4) {
            val name = parts[0]
            val version = parts[1].toIntOrNull()
            val screenWidth = parts[2].toIntOrNull()
            val screenHeight = parts[3].toIntOrNull()

            if (version == null || screenWidth == null || screenHeight == null || name.isEmpty()) {
                "Invalid Parameters".toByteArray()
            } else {
                val gameClient = ccp.addClient(name, version, screenWidth, screenHeight)
                byteArrayOf(gameClient.id.toByte())
            }
        } else {
            "Incorrect data format, should have: 'name,version,width,height'".toByteArray()
        }
    }
}
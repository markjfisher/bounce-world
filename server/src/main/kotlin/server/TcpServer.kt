package server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import command.ClientCommandProcessor
import command.ShapesCommandProcessor
import command.WorldCommandProcessor
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import logger

private val objectMapper: ObjectMapper = jacksonObjectMapper()

private fun serializeObjectToByteArray(obj: Any): ByteArray {
    // Convert the object to a JSON string and then to a ByteArray
    return objectMapper.writeValueAsBytes(obj)
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

        try {
            val command: String? = try {
                withTimeoutOrNull(1000) {
                    input.readUTF8Line()
                }
            } catch (e: TimeoutCancellationException) {
                logger.error("Timed out waiting for command from client at ${socket.remoteAddress}")
                null
            }

            if (command == null) {
                logger.error("No client command to process")
            } else {
                val response = process(command)
                output.writeByteArray(response)
            }
        } catch (e: Throwable) {
            logger.error("Error while handling client", e)
        } finally {
            withContext(scope.coroutineContext) {
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
            command == "msg" -> serializeObjectToByteArray(wcp.getLatestMessage())
            command == "who" -> serializeObjectToByteArray(wcp.getClients())
            command.startsWith("new-body ") -> doNewBody(rp(command))
            command.startsWith("add-body ") -> doAddBody(rp(command))
            command.startsWith("cmd-put ") -> doClientCommand(rp(command))
            command.startsWith("cmd-get ") -> doFetchCommands(rp(command))
            command.startsWith("cmd-broadcast ") -> doBroadcastCommand(rp(command))

            // CLIENT commands
            command.startsWith("add-client ") -> doAddClient(rp(command))

            // SHAPES commands
            command == "shape-data" -> scp.getShapesData()
            command == "shape-count" -> serializeObjectToByteArray(scp.getShapesCount())
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
                serializeObjectToByteArray(gameClient.id)
            }
        } else {
            "Incorrect data format, should have: 'name,version,width,height'".toByteArray()
        }
    }
}
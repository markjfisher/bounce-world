package server

import command.ClientCommandProcessor
import command.ShapesCommandProcessor
import command.WorldCommandProcessor
import extensions.serializeObjectToByteArray
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
import logger
import java.io.IOException

class TcpServer(
    private val wcp: WorldCommandProcessor,
    private val ccp: ClientCommandProcessor,
    private val scp: ShapesCommandProcessor,
    private val host: String,
    private val port: Int,
    private val scope: CoroutineScope,
) {
    fun start() {
        logger.info("Starting TCP server on interface: $host, port: $port")
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
                // allow client 5s to send something
                val bytesRead: Int? = withTimeoutOrNull(5_000) {
                    input.readAvailable(buffer, 0, buffer.size)
                }

                if (bytesRead == null) {
                    logger.info("No client command to process, exiting")
                    break
                } else if (bytesRead == -1 || input.isClosedForRead) {
                    logger.info("client connection closed")
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
                    // logger.info("sending client data len: ${response.size} (${String.format("%02x", response.size)}):\n${response.toCustomHexDump()}")

                    if (commandString.startsWith("close")) {
                        isKeepActive = false
                    }
                }
            }
        } catch (e: Throwable) {
            when (e) {
                is IOException -> logger.error("IO error from client: ${e.message}")
                else -> logger.error("Error while handling client", e)
            }
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
        // logger.info("Processing command: >$command<\n${command.toByteArray(Charsets.UTF_8).toCustomHexDump()}")

        return when {
            // WORLD commands
            command.startsWith("w ") -> doGetWorldData(rp(command))
            command == "ws" -> wcp.getWorldState()
            command == "status" -> serializeObjectToByteArray(wcp.getStatus())
            command == "reset" -> wcp.resetWorld()
            command == "inc" -> wcp.increaseSpeed()
            command == "dec" -> wcp.decreaseSpeed()
            command == "freeze" -> wcp.toggleFreeze()
            command == "msg" -> min1(wcp.getLatestMessage())
            command == "who" -> min1(wcp.who())
            command.startsWith("close") -> wcp.close(rp(command))
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

    private fun min1(s: String): ByteArray {
        return if (s.isNotEmpty()) {
            s.toByteArray(Charsets.UTF_8)
        } else {
            byteArrayOf(0)
        }
    }

    private fun doGetWorldData(arg: String): ByteArray {
        val clientId = arg.toIntOrNull()
        if (clientId == null) {
            logger.error("No client id found for >$arg<")
            return byteArrayOf(0)
        }
        return wcp.getWorldData(clientId)
    }

    private fun doNewBody(arg: String): ByteArray {
        val (shapeIdString, clientIdString) = arg.split(',', limit = 2)
        val shapeId = shapeIdString.toIntOrNull()
        val clientId = clientIdString.toIntOrNull()
        return if (shapeId != null && clientId != null) {
            wcp.addBody(shapeId, clientId)
        } else {
            logger.error("Invalid size parameter: >$arg<")
            byteArrayOf(0)
        }
    }

    private fun doAddBody(arg: String): ByteArray {
        val size = arg.toIntOrNull()// ?: return "Invalid size parameter: $arg".toByteArray()
        if (size == null) {
            logger.error("Invalid size parameter: >$arg<")
            return byteArrayOf(0)
        }
        return wcp.addRandomBodyWithSize(size)
    }

    private fun doClientCommand(arg: String): ByteArray {
        // cmd-put clientId,cmd  # the clientId can be "ALL" for all clients or just their id value
        val (clientId, cmd) = arg.split(',', limit = 2)
        if (clientId.isEmpty() || cmd.isEmpty()) {
            logger.error("Invalid Command: $arg, should be in format 'cmd-put clientId,cmd'")
            return byteArrayOf(0)
        }
        return wcp.clientCommand(clientId, cmd)
    }

    private fun doFetchCommands(arg: String): ByteArray {
        val clientId = arg.toIntOrNull()
        if (clientId == null) {
            logger.error("Invalid clientID: >$arg<")
            return byteArrayOf(0)
        }
        return wcp.fetchCommands(arg)
    }

    private fun doBroadcastCommand(arg: String): ByteArray {
        // cmd-broadcast [clientId|ALL],time,message string
        val parts = arg.split(',', limit = 3)
        return if (parts.size == 3) {
            val (clientId, time, message) = parts
            wcp.broadcastCommand(clientId, time, message)
        } else {
            logger.error("Broadcast Command: invalid parameters $arg")
            return byteArrayOf(0)
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
                logger.error("Invalid Parameters given: >$arg<")
                byteArrayOf(0)
            } else {
                val gameClient = ccp.addClient(name, version, screenWidth, screenHeight)
                byteArrayOf(gameClient.id.toByte())
            }
        } else {
            logger.error("Incorrect data format, should have: 'name,version,width,height'")
            byteArrayOf(0)
        }
    }
}
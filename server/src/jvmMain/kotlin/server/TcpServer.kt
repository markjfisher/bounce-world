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
    private val loggingRequests: Boolean,
    private val scope: CoroutineScope,
) {
    companion object {
        // Idle timeout waiting for the next TCP read
        private const val READ_TIMEOUT_MS = 10_000L
    }
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
        val lineBuffer = TcpLineBuffer()

        logger.info("TCP client connected from ${socket.remoteAddress}")

        try {
            while (isKeepActive) {
                val buffer = ByteArray(1024)
                val bytesRead: Int? = withTimeoutOrNull(READ_TIMEOUT_MS) {
                    input.readAvailable(buffer, 0, buffer.size)
                }

                if (bytesRead == null) {
                    if (lineBuffer.hasPending()) {
                        logger.warn(
                            "TCP read timeout with incomplete command; pending=${lineBuffer.pendingDebug()}",
                        )
                    } else {
                        logger.info("TCP read timeout with no data received, closing connection")
                    }
                    break
                } else if (bytesRead == -1 || input.isClosedForRead) {
                    if (lineBuffer.hasPending()) {
                        logger.warn(
                            "TCP client closed with incomplete command; pending=${lineBuffer.pendingDebug()}",
                        )
                    } else {
                        logger.info("TCP client connection closed")
                    }
                    break
                } else if (bytesRead == 0) {
                    continue
                } else {
                    if (loggingRequests) {
                        logger.info(
                            "TCP << $bytesRead bytes: ${buffer.formatForTcpLog(bytesRead)}",
                        )
                    }
                    val commandLines = lineBuffer.append(buffer, bytesRead)
                    if (loggingRequests && commandLines.isEmpty() && lineBuffer.hasPending()) {
                        logger.info("TCP awaiting remainder of command; pending=${lineBuffer.pendingDebug()}")
                    }
                    for (commandString in commandLines) {
                        if (loggingRequests) {
                            logger.info("TCP command: >$commandString<")
                        }
                        isKeepActive = commandString.startsWith("x-")
                        val response = processCommand(commandString.substringAfter("x-").trim())
                        if (loggingRequests) {
                            logger.info(
                                "TCP >> ${response.size} bytes: ${response.formatForTcpLog(response.size)}",
                            )
                        }
                        output.writeByteArray(response)

                        if (commandString.startsWith("close")) {
                            isKeepActive = false
                            break
                        }
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

    internal fun processCommand(command: String): ByteArray {
        val commandRegex = """^[a-zA-Z-]+\s""".toRegex()

        // rp == remove prefix
        fun rp(command: String): String = command.replaceFirst(commandRegex, "")
        // logger.info("Processing command: >$command<\n${command.toByteArray(Charsets.UTF_8).toCustomHexDump()}")

        return when {
            // WORLD commands
            command.startsWith("w ") -> doGetWorldData(rp(command))
            command.startsWith("d ") -> doGetWorldDataWithSize(rp(command))
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

    private fun doGetWorldDataWithSize(arg: String): ByteArray {
        val clientId = arg.toIntOrNull()
        if (clientId == null) {
            logger.error("No client id found for >$arg<")
            return WorldCommandProcessor.prependPacketSize(byteArrayOf(0))
        }
        return wcp.getWorldDataWithSize(clientId)
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

    private fun ByteArray.formatForTcpLog(length: Int): String {
        val hex = take(length).joinToString(" ") { byte -> "%02x".format(byte) }
        val text = take(length).joinToString("") { byte ->
            when (byte.toUByte().toInt()) {
                in 0x20..0x7e -> byte.toInt().toChar().toString()
                0x0a -> "\\n"
                0x0d -> "\\r"
                0x9b -> "\\x9b"
                else -> "."
            }
        }
        return "hex=[$hex] text=[$text]"
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
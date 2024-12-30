package server

import command.CommandProcessor
import config.WorldConfig
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

class TcpServer(
    private val commandProcessor: CommandProcessor,
    private val worldConfig: WorldConfig,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            val serverSocket = aSocket(SelectorManager(this.coroutineContext)).tcp().bind(worldConfig.tcpHost, worldConfig.tcpPort)
            while (true) {
                val socket = serverSocket.accept()
                // By launching here, a new coroutine handles this connection, so the server is immediately free to handle a new connection
                // which is how we achieve high concurrency
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
                // Use withTimeoutOrNull to attempt reading a line from client with short timeout
                withTimeoutOrNull(1000) {
                    input.readUTF8Line()
                }
            } catch (e: TimeoutCancellationException) {
                println("Timed out waiting for command from client at ${socket.remoteAddress}")
                null
            }

            if (command == null) {
                println("No client command to process")
            } else {
                val response = commandProcessor.process(command)
                output.writeByteArray(response)
            }
        } catch (e: Throwable) {
            println("Error while handling client: ${e.message}")
        } finally {
            withContext(scope.coroutineContext) {
                socket.close()
            }
        }
    }
}
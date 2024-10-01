package handler

import command.CommandProcessor
import io.ktor.network.sockets.*
import io.ktor.utils.io.*

class SocketHandler(worldConfig: WorldConfig) {
    private val commandProcessor = CommandProcessor(worldConfig)

    suspend fun handle(socket: Socket) {
        println("Accepted connection from ${socket.remoteAddress}")
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)

        try {
            while (true) {
                val command = input.readUTF8Line()
                if (command == null) {
                    println("Client disconnected")
                    break
                }
                val response = commandProcessor.process(command)
                output.writeByteArray(response)
            }
        } catch (e: Throwable) {
            println("Error while handling client: ${e.message}")
        } finally {
            socket.close()
        }
    }
}

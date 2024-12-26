package server

import command.CommandProcessor
import config.WorldConfig
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeByteArray
import javafx.application.Application.launch
import kotlinx.coroutines.*
import java.net.InetSocketAddress

class TcpServer(
    private val commandProcessor: CommandProcessor,
    private val worldConfig: WorldConfig
) {
    suspend fun start() {
       val socketAddress = InetSocketAddress(worldConfig.tcpHost, worldConfig.tcpPort)
       val serverSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().bind(socketAddress)
       println("Server is listening at ${serverSocket.localAddress}")

       while (true) {
           val socket = serverSocket.accept()
           launch {
               handleClient(socket)
           }
       }
    }

   private suspend fun handleClient(socket: Socket) {
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
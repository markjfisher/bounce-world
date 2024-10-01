package server

import config.WorldConfig
import handler.SocketHandler
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import java.net.InetSocketAddress

class TcpServer(private val worldConfig: WorldConfig) {
    private val socketHandler = SocketHandler(worldConfig)

    suspend fun start() {
        val serverSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().bind(
            InetSocketAddress(worldConfig.tcpHost, worldConfig.tcpPort)
        )
        println("Server is listening at ${serverSocket.localAddress}")
        
        while (true) {
            val socket = serverSocket.accept()
            launch {
                socketHandler.handle(socket)
            }
        }
    }
}
package bw

import bw.Model.clients
import domain.WorldUpdateListener
import domain.toWorldShared
import io.ktor.server.application.ApplicationCall
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import logger
import java.util.concurrent.ConcurrentHashMap

class BouncyService(private val call: ApplicationCall) : IBouncyService {
    override suspend fun getWorldData(): WorldShared {
        return call.application.attributes[WorldAttributeKey].toWorldShared()
    }

    override suspend fun getClients(): List<GameClientShared> {
        TODO("Not yet implemented")
    }

    override suspend fun getShapes(): List<ShapeShared> {
        TODO("Not yet implemented")
    }
}

object Model {
    val clients: ConcurrentHashMap.KeySetView<SendChannel<WorldShared>, Boolean> = ConcurrentHashMap.newKeySet()
}

class BouncyWsService(private val wsSession: WebSocketServerSession) : IBouncyWsService, WorldUpdateListener {
    private var hasRegistered = false

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun socketConnection(input: ReceiveChannel<Int>, output: SendChannel<WorldShared>) {
        val world = wsSession.call.application.attributes[WorldAttributeKey]
        if (!hasRegistered) {
            world.addListener(this)
            hasRegistered = true
        }

        clients.retainAll { !it.isClosedForSend }
        clients.add(output)
        output.send(world.toWorldShared())

        try {
            while (!output.isClosedForSend && !input.isClosedForReceive) {
                val signal = withTimeoutOrNull(100) {
                    input.receiveCatching().getOrNull()
                }

                if (signal != null) {
                    logger.info("web client sent signal: $signal")
                } else {
                    delay(1000)
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            logger.info("web client closed: $e")
        } finally {
            clients.remove(output)
            logger.info("Client connection removed")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun update(state: WorldShared) {
        clients.retainAll { !it.isClosedForSend }
        clients.forEach { client ->
            if (!client.isClosedForSend) {
                client.send(state)
            }
        }
    }
}

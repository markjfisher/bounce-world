package bw

import bw.Model.clients
import com.google.inject.Inject
import domain.WorldUpdateListener
import domain.toWorldShared
import io.ktor.server.application.ApplicationCall
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import logger

actual class BouncyService : IBouncyService {
    @Inject
    lateinit var call: ApplicationCall

    override suspend fun getWorldData() = call.application.attributes[WorldAttributeKey].toWorldShared()

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

actual class BouncyWsService : IBouncyWsService, WorldUpdateListener {
    @Inject
    lateinit var wsSession: WebSocketServerSession

    private val keepaliveScope = CoroutineScope(Dispatchers.IO)

    private var hasRegistered = false

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun socketConnection(input: ReceiveChannel<Int>, output: SendChannel<WorldShared>) {
        val world = wsSession.call.application.attributes[WorldAttributeKey]
        if (!hasRegistered) {
            world.addListener(this)
            hasRegistered = true
        }

        // remove any dead connections before adding the new one. In all likelihood, it was the existing connection with a refresh, and thus a new connection replacing the old one.
        clients.retainAll { !it.isClosedForSend }
        clients.add(output)
        // immediately populate the new client
        output.send(world.toWorldShared())

        // have to keep the channel alive by listening to input messages.
        try {
            // when a client refreshes, it's actually the input channel that is closed, not the output, but keep both here in case the output channel closes for some reason
            while (!output.isClosedForSend && !input.isClosedForReceive) {
                // small timeout on receive so we don't hang waiting for inputs (at the moment there are non from web app)
                val signal = withTimeoutOrNull(100) {
                    input.receiveOrNull()
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

    private suspend fun ReceiveChannel<Int>.receiveOrNull(): Int? = try {
        receive()
    } catch (e: ClosedReceiveChannelException) {
        null
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun update(state: WorldShared) {
        // check for any clients that were closed first, so we don't send things to non-open connections
        clients.retainAll { !it.isClosedForSend }
        clients.forEach { client ->
            if (!client.isClosedForSend) {
                client.send(state)
            }
        }
    }
}
package bw

import bw.Model.clients
import com.google.inject.Inject
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.util.concurrent.ConcurrentHashMap

actual class BouncyService : IBouncyService {
    @Inject
    lateinit var call: ApplicationCall

    override suspend fun getWorldData(): WorldShared {
        val world = call.application.attributes[WorldAttributeKey]
        return WorldShared(
            width = world.getWorldWidth(),
            height = world.getWorldHeight(),
            upTime = "TBD",
            clients = world.clients().associate { client ->
                client.id to GameClientShared(
                    id = client.id,
                    name = client.name,
                    version = client.version,
                    position = Pair(client.position.x, client.position.y),
                    screenSize = Pair(client.screenSize.width, client.screenSize.height)
                )
            },
            isFrozen = world.isFrozen,
            isWrapping = world.isWrapping,
            bodies = world.currentSimulator.bodies.map { body ->
                BodyShared(
                    id = body.id,
                    position = Pair(body.position.x, body.position.y),
                    velocity = Pair(body.velocity.x, body.velocity.y),
                    mass = body.mass,
                    radius = body.radius,
                    shapeId = body.shapeId
                )
            }
        )
    }

    override suspend fun getClients(): List<GameClientShared> {
        TODO("Not yet implemented")
    }

    override suspend fun getShapes(): List<ShapeShared> {
        TODO("Not yet implemented")
    }
}

object Model {
    val clients = ConcurrentHashMap.newKeySet<SendChannel<WorldShared>>()
}

actual class BouncyWsService : IBouncyWsService {
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun socketConnection(input: ReceiveChannel<Int>, output: SendChannel<WorldShared>) {
        if (output.isClosedForSend || input.isClosedForReceive) {
            println("removing socketConnection")
            clients.remove(output)
            return
        }
        println("got a socketConnection, existing? $output")
        clients.add(output)
    }
}
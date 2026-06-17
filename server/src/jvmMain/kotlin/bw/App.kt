package bw

import command.ClientCommandProcessor
import command.ShapesCommandProcessor
import command.WorldCommandProcessor
import config.WorldConfig
import dev.kilua.rpc.applyRoutes
import dev.kilua.rpc.getServiceManager
import dev.kilua.rpc.initRpc
import dev.kilua.rpc.registerService
import domain.World
import factory.WorldFactory
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.util.AttributeKey
import io.kvision.remote.registerRemoteTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import logger
import routing.clientRouting
import routing.shapesRouting
import routing.worldRouting
import server.TcpServer
import java.util.Date

val WorldCommandProcessorAttributeKey = AttributeKey<WorldCommandProcessor>("WorldCommandProcessor")
val ClientCommandProcessorAttributeKey = AttributeKey<ClientCommandProcessor>("ClientCommandProcessor")
val ShapesCommandProcessorAttributeKey = AttributeKey<ShapesCommandProcessor>("ShapesCommandProcessor")
val WorldAttributeKey = AttributeKey<World>("World")

fun main() = runBlocking {
    registerRemoteTypes()
    logger.info("Starting Bouncy World Service at ${Date()}")
    val env = applicationEnvironment {
        config = ApplicationConfig("application.conf")
    }
    val worldConfig = WorldConfig(env.config)
    val world = WorldFactory.create(worldConfig)

    val worldCommandProcessor = WorldCommandProcessor(world, worldConfig)
    val clientCommandProcessor = ClientCommandProcessor(world)
    val shapesCommandProcessor = ShapesCommandProcessor(world)

    launch(Dispatchers.IO) {
        val tcpServer = TcpServer(
            worldCommandProcessor,
            clientCommandProcessor,
            shapesCommandProcessor,
            worldConfig.tcpHost,
            worldConfig.tcpPort,
            worldConfig.loggingRequests,
            prependResponseSize = false,
            this,
        )
        tcpServer.start()
    }

    launch(Dispatchers.IO) {
        val framedTcpServer = TcpServer(
            worldCommandProcessor,
            clientCommandProcessor,
            shapesCommandProcessor,
            worldConfig.tcpHost,
            worldConfig.tcpFramedPort,
            worldConfig.loggingRequests,
            prependResponseSize = true,
            this,
        )
        framedTcpServer.start()
    }

    embeddedServer(factory = Netty, environment = env, configure = { envConfig(env) }, module = {
        attributes.put(WorldCommandProcessorAttributeKey, worldCommandProcessor)
        attributes.put(ClientCommandProcessorAttributeKey, clientCommandProcessor)
        attributes.put(ShapesCommandProcessorAttributeKey, shapesCommandProcessor)
        attributes.put(WorldAttributeKey, world)

        worldModule()
        clientModule()
        shapesModule()
        kvisionModule()
    }).start(wait = true)

    Unit
}

fun ApplicationEngine.Configuration.envConfig(env: ApplicationEnvironment) {
    connector {
        host = env.config.property("ktor.deployment.host").getString()
        port = env.config.property("ktor.deployment.port").getString().toInt()
    }
}

fun Application.worldModule() {
    worldRouting()
}

fun Application.clientModule() {
    clientRouting()
}

fun Application.shapesModule() {
    shapesRouting()
}

fun Application.kvisionModule() {
    logger.info("Initialising Kilua RPC services for web client")
    install(Compression)
    install(WebSockets)
    routing {
        applyRoutes(getServiceManager<IBouncyService>())
        applyRoutes(getServiceManager<IBouncyWsService>())
    }
    initRpc {
        registerService<IBouncyService> { call -> BouncyService(call) }
        registerService<IBouncyWsService> { _, wsSession -> BouncyWsService(wsSession) }
    }
}

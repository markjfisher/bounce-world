package bw

import command.ClientCommandProcessor
import command.ShapesCommandProcessor
import command.WorldCommandProcessor
import config.WorldConfig
import data.ShapeCreator
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
import io.kvision.remote.applyRoutes
import io.kvision.remote.getServiceManager
import io.kvision.remote.kvisionInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import logger
import routing.clientRouting
import routing.shapesRouting
import routing.worldRouting
import server.TcpServer
import java.util.Date

// the keys to store values in the Attributes framework. I like this!
val WorldCommandProcessorAttributeKey = AttributeKey<WorldCommandProcessor>("WorldCommandProcessor")
val ClientCommandProcessorAttributeKey = AttributeKey<ClientCommandProcessor>("ClientCommandProcessor")
val ShapesCommandProcessorAttributeKey = AttributeKey<ShapesCommandProcessor>("ShapesCommandProcessor")
val WorldAttributeKey = AttributeKey<World>("World")

fun main() = runBlocking {
    logger.info("Starting Bouncy World Service at ${Date()}")
    val env = applicationEnvironment {
        config = ApplicationConfig("application.conf")
    }
    val worldConfig = createWorldConfig(env.config)
    val world = WorldFactory.create(worldConfig)
    val shapes = ShapeCreator.createShapes()
    world.setShapes(shapes)

    val worldCommandProcessor = WorldCommandProcessor(world, worldConfig)
    val clientCommandProcessor = ClientCommandProcessor(world)
    val shapesCommandProcessor = ShapesCommandProcessor(world)

    // Create a tcp listener in its own thread/coroutine. We can't use Attributes in this side, as it's not part of the ktor framework, as such
    // so we inject the commandProcessor directly as a dependency.
    launch(Dispatchers.IO) {
        val tcpServer = TcpServer(
            worldCommandProcessor,
            clientCommandProcessor,
            shapesCommandProcessor,
            worldConfig.tcpHost,
            worldConfig.tcpPort,
            this
        )
        tcpServer.start()
    }

    embeddedServer(factory = Netty, environment = env, configure = { envConfig(env) }, module = {
        // ktor uses attributes for storing and retrieving state without passing it via parameters
        attributes.put(WorldCommandProcessorAttributeKey, worldCommandProcessor)
        attributes.put(ClientCommandProcessorAttributeKey, clientCommandProcessor)
        attributes.put(ShapesCommandProcessorAttributeKey, shapesCommandProcessor)
        // for the web client
        attributes.put(WorldAttributeKey, world)

        worldModule()
        clientModule()
        shapesModule()
        kvisionModule()
    }).start(wait = true)

    // required by runBlocking<Unit>, we have to exit with no value, but the start function above returns one
    Unit
}

fun ApplicationEngine.Configuration.envConfig(env: ApplicationEnvironment) {
    connector {
        host = env.config.property("ktor.deployment.host").getString()
        port = env.config.property("ktor.deployment.port").getString().toInt()
    }
}

// referenced in Test, and in main. It was referenced in application.conf:ktor.application.modules, but I've moved to a completely code driven setup
fun Application.worldModule() {
    // The idea of modules is to group any functionality we need to setup here. At the moment there is only routing
    worldRouting()
}

fun Application.clientModule() {
    clientRouting()
}

fun Application.shapesModule() {
    shapesRouting()
}

fun Application.kvisionModule() {
    logger.info("Initialising kvision services for web client")
    install(Compression)
    install(WebSockets)
    routing {
        applyRoutes(getServiceManager<IBouncyService>())
        applyRoutes(getServiceManager<IBouncyWsService>())
    }
    kvisionInit()
}

fun createWorldConfig(config: ApplicationConfig): WorldConfig {
    return WorldConfig(
        width = config.property("world.width").getString().toInt(),
        height = config.property("world.height").getString().toInt(),
        updatesPerSecond = config.property("world.updatesPerSecond").getString().toInt(),
        shouldAutoStart = config.property("world.shouldAutoStart").getString().toBoolean(),
        initialSpeed = config.property("world.initialSpeed").getString().toFloat(),
        heartbeatTimeoutMillis = config.property("world.heartbeatTimeoutMillis").getString().toLong(),
        locationPattern = config.property("world.locationPattern").getString(),
        enableWrapping = config.property("world.enableWrapping").getString().toBoolean(),
        tcpHost = config.property("world.tcp.host").getString(),
        tcpPort = config.property("world.tcp.port").getString().toInt(),
    )
}
package bw

import command.ClientCommandProcessor
import command.ShapesCommandProcessor
import command.WorldCommandProcessor
import config.WorldConfig
import factory.WorldFactory
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.util.AttributeKey
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

fun main() = runBlocking {
    logger.info("Starting Bounce World Service at ${Date()}")
    val env = applicationEnvironment {
        config = ApplicationConfig("application.conf")
    }
    val worldConfig = WorldConfig(env.config)
    val world = WorldFactory.create(worldConfig)

    val worldCommandProcessor = WorldCommandProcessor(world, worldConfig)
    val clientCommandProcessor = ClientCommandProcessor(world)
    val shapesCommandProcessor = ShapesCommandProcessor(world)

    // Create a tcp listener in its own thread/coroutine. We can't use Attributes in this side, as it's not part of the ktor framework, as such
    // so we inject the commandProcessor directly as a dependency.
    launch(Dispatchers.IO) {
        val tcpServer = TcpServer(worldCommandProcessor, worldConfig, this)
        tcpServer.start()
    }

    embeddedServer(factory = Netty, environment = env, configure = { envConfig(env) }, module = {
        // ktor uses attributes for storing and retrieving state without passing it via parameters
        attributes.put(WorldCommandProcessorAttributeKey, worldCommandProcessor)
        attributes.put(ClientCommandProcessorAttributeKey, clientCommandProcessor)
        attributes.put(ShapesCommandProcessorAttributeKey, shapesCommandProcessor)

        commonModule()
        worldModule()
        clientModule()
        shapesModule()
    }).start(wait = true)

    // required by runBlocking<Unit>, we have to exit with no value, but the start function above returns a value
    Unit
}

fun ApplicationEngine.Configuration.envConfig(env: ApplicationEnvironment) {
    connector {
        host = env.config.property("ktor.deployment.host").getString()
        port = env.config.property("ktor.deployment.port").getString().toInt()
    }
}

fun Application.commonModule() {
    install(ContentNegotiation) {
        jackson {
            //// pretty print:
            // enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
            //// To include non-null values only:
            // setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        }
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
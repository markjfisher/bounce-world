package bw

import command.CommandProcessor
import config.WorldConfig
import domain.World
import factory.WorldFactory
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.AttributeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import routing.configureRouting
import server.TcpServer

// the keys to store values in the Attributes framework. I like this!
val CommandProcessorAttributeKey = AttributeKey<CommandProcessor>("CommandProcessor")
val WorldAttributeKey = AttributeKey<World>("World")
val WorldConfigAttributeKey = AttributeKey<WorldConfig>("WorldConfig")

fun main() = runBlocking {
    val env = applicationEnvironment {
        config = ApplicationConfig("application.conf")
    }
    val worldConfig = WorldConfig(env.config)
    val world = WorldFactory.create(worldConfig)
    val commandProcessor = CommandProcessor(world)

    // Create a tcp listener in its own thread/coroutine. We can't use Attributes in this side, as it's not part of the ktor framework, as such
    // so we inject the commandProcessor directly as a dependency.
    launch(Dispatchers.IO) {
        val tcpServer = TcpServer(commandProcessor, worldConfig, this)
        tcpServer.start()
    }

    embeddedServer(
        factory = Netty,
        environment = env,
        configure = { envConfig(env) },
        module = {
            // ktor uses attributes for storing and retrieving state without passing it via parameters
            attributes.put(CommandProcessorAttributeKey, commandProcessor)
            attributes.put(WorldAttributeKey, world)
            attributes.put(WorldConfigAttributeKey, worldConfig)
            // here's how we invoke the module function. There could be more modules and just run them here like this
            module()
        }
    ).start(wait = true)

    // required by runBlocking<Unit>, we have to exit with no value, but the start function above returns a value
    Unit
}

fun ApplicationEngine.Configuration.envConfig(env: ApplicationEnvironment) {
    println("doing envConfig")
    connector {
        host = env.config.property("ktor.deployment.host").getString()
        port = env.config.property("ktor.deployment.port").getString().toInt()
    }
}

// referenced in Test, and in main. It was referenced in application.conf:ktor.application.modules, but I've moved to a completely code driven setup
fun Application.module() {
    configureRouting()
}

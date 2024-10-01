import config.WorldConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.http.*
import command.CommandProcessor
import server.TcpServer
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val env = applicationEngineEnvironment {
        config = ApplicationConfig("application.conf")
        module {
            val worldConfig = WorldConfig(environment.config)
            configureRouting(worldConfig)
        }
    }

    embeddedServer(Netty, env).start(wait = false)

    val worldConfig = WorldConfig(env.config)
    val tcpServer = TcpServer(worldConfig)
    runBlocking {
        tcpServer.start()
    }
}

fun Application.module() {
    val worldConfig = WorldConfig(environment.config)
    // Use worldConfig here to configure your application
}

fun Application.configureRouting(worldConfig: WorldConfig) {
    val commandProcessor = CommandProcessor(worldConfig)

    routing {
        get("/add/{size}") {
            val size = call.parameters["size"]?.toIntOrNull()
            if (size != null) {
                val response = commandProcessor.process("add/$size")
                call.respondBytes(response)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid size parameter")
            }
        }

        get("/reset") {
            val response = commandProcessor.process("reset")
            call.respondBytes(response)
        }

        get("/inc") {
            val response = commandProcessor.process("inc")
            call.respondBytes(response)
        }

        get("/dec") {
            val response = commandProcessor.process("dec")
            call.respondBytes(response)
        }

        get("/cmd/put/{clientId}/{cmd}") {
            val clientId = call.parameters["clientId"]
            val cmd = call.parameters["cmd"]
            if (clientId != null && cmd != null) {
                val response = commandProcessor.process("cmd/put/$clientId/$cmd")
                call.respondBytes(response)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
            }
        }

        get("/cmd/broadcast/{clientId}/{time}/{message}") {
            val clientId = call.parameters["clientId"]
            val time = call.parameters["time"]
            val message = call.parameters["message"]
            if (clientId != null && time != null && message != null) {
                val response = commandProcessor.process("cmd/broadcast/$clientId/$time/$message")
                call.respondBytes(response)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
            }
        }

        get("/freeze") {
            val response = commandProcessor.process("freeze")
            call.respondBytes(response)
        }

        get("/status") {
            val response = commandProcessor.process("status")
            call.respondBytes(response)
        }

        get("/w/{clientId}") {
            val clientId = call.parameters["clientId"]?.toIntOrNull()
            if (clientId != null) {
                val response = commandProcessor.process("w/$clientId")
                call.respondBytes(response)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid clientId parameter")
            }
        }

        get("/ws") {
            val response = commandProcessor.process("ws")
            call.respondBytes(response)
        }
    }
}

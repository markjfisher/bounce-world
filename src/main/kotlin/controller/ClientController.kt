package controller

import domain.GameClient
import domain.GameClientInfo
import domain.World
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import jakarta.validation.Valid
import java.util.UUID

@Controller("/client")
@ExecuteOn(TaskExecutors.BLOCKING)
open class ClientController(private val world: World) {
    @Post(produces = [APPLICATION_JSON], consumes = [APPLICATION_JSON])
    open fun registerClient(@Valid @Body gameClientInfo: GameClientInfo): HttpResponse<GameClient> {
        val clientId = UUID.randomUUID().toString().substring(0, 8)
        val gameClient = GameClient(
            id = clientId,
            name = gameClientInfo.name,
            version = gameClientInfo.version,
            screenSize = gameClientInfo.screenSize
        )
        world.addClient(gameClient)
        return HttpResponse.created(gameClient)
    }
}

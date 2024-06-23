package controller

import domain.GameClient
import domain.GameClientInfo
import domain.World
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import java.util.UUID

@Controller("/clients")
class ClientController(private val world: World) {

    @Post(produces = [APPLICATION_JSON], consumes = [APPLICATION_JSON])
    fun registerClient(@Body gameClientInfo: GameClientInfo): HttpResponse<GameClient> {
        val clientId = UUID.randomUUID().toString()
        val gameClient = GameClient(
            id = clientId,
            name = gameClientInfo.name,
            screenSize = gameClientInfo.screenSize
        )
        world.addClient(gameClient)
        return HttpResponse.created(gameClient)
    }
}

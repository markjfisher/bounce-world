package controller

import domain.GameClient
import domain.GameClientInfo
import domain.World
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.MediaType.TEXT_PLAIN
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import jakarta.validation.Valid

@Controller("/client")
@ExecuteOn(TaskExecutors.BLOCKING)
open class ClientController(private val world: World) {
    @Post(produces = [TEXT_PLAIN], consumes = [APPLICATION_JSON])
    open fun registerClient(@Valid @Body gameClientInfo: GameClientInfo): HttpResponse<String> {
        val gameClient = world.createClient(gameClientInfo)
        return HttpResponse.ok(gameClient.id)
    }
}

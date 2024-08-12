package controller

import domain.GameClientInfo
import domain.ScreenSize
import domain.World
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.APPLICATION_FORM_URLENCODED
import io.micronaut.http.MediaType.TEXT_PLAIN
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@Controller("/client")
@ExecuteOn(TaskExecutors.BLOCKING)
open class ClientController(private val world: World) {
    // Allow simple comma separated fields so the client doesn't have to write JSON for the sake of it
    @Post(produces = [TEXT_PLAIN], consumes = [TEXT_PLAIN, APPLICATION_FORM_URLENCODED])
    open fun registerClient(@Body gameClientInfoString: String): HttpResponse<String> {
        val parts = gameClientInfoString.split(",")
        if (parts.size != 4) {
            return HttpResponse.badRequest("Incorrect data format")
        }
        val gameClientInfo = GameClientInfo(
            name = parts[0],
            version = parts[1].toInt(),
            screenSize = ScreenSize(parts[2].toInt(), parts[3].toInt())
        )
        val gameClient = world.createClient(gameClientInfo)
        println("Created client: $gameClient")
        return HttpResponse.created(gameClient.id)
    }
}

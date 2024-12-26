package controller

import domain.GameClientInfo
import domain.ScreenSize
import domain.World

// @ExecuteOn(TaskExecutors.BLOCKING)
// open class ClientController(private val world: World) {
//     // Allow simple comma separated fields so the client doesn't have to write JSON for the sake of it
//     @Post(produces = [APPLICATION_OCTET_STREAM], consumes = [TEXT_PLAIN, APPLICATION_FORM_URLENCODED])
//     open fun registerClient(@Body gameClientInfoString: String): HttpResponse<Any> {
//         val parts = gameClientInfoString.split(",")
//         if (parts.size != 4) {
//             return HttpResponse.badRequest("Incorrect data format")
//         }
//         val gameClientInfo = GameClientInfo(
//             name = parts[0],
//             version = parts[1].toInt(),
//             screenSize = ScreenSize(parts[2].toInt(), parts[3].toInt())
//         )
//         val gameClient = world.createClient(gameClientInfo)
//         println("Created client: $gameClient")
//         return HttpResponse.created(byteArrayOf(gameClient.id.toByte()))
//     }
// }

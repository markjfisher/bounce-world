package command

import domain.GameClient
import domain.GameClientInfo
import domain.ScreenSize
import domain.World

class ClientCommandProcessor(private val world: World) {
    fun addClient(name: String, version: Int, screenWidth: Int, screenHeight: Int): GameClient {
        val gameClientInfo = GameClientInfo(
            name = name,
            version = version,
            screenSize = ScreenSize(screenWidth, screenHeight)
        )
        val gameClient = world.createClient(gameClientInfo)
        println("Created client: $gameClient")
        return gameClient
    }
}
package command

import domain.GameClient
import domain.GameClientInfo
import domain.ScreenSize
import domain.World
import logger

class ClientCommandProcessor(private val world: World) {
    fun addClient(
        name: String,
        version: Int,
        screenWidth: Int,
        screenHeight: Int,
        worldWidth: Int? = null,
        worldHeight: Int? = null,
    ): GameClient {
        val gameClientInfo = GameClientInfo(
            name = name,
            version = version,
            screenSize = ScreenSize(screenWidth, screenHeight),
            worldSize = if (worldWidth != null && worldHeight != null) {
                ScreenSize(worldWidth, worldHeight)
            } else {
                null
            },
        )
        val gameClient = world.createClient(gameClientInfo)
        logger.info("Created client: $gameClient")
        return gameClient
    }
}

package bw

import dev.kilua.rpc.getService
import io.kvision.state.ObservableList
import io.kvision.state.ObservableValue
import io.kvision.state.observableListOf
import io.kvision.utils.syncWithList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Model {
    private val bouncyService = getService<IBouncyService>()
    private val wsBouncyService = getService<IBouncyWsService>()

    val worldData = ObservableValue(WorldShared())
    val clients: ObservableList<GameClientShared> = observableListOf()
    val shapes: ObservableList<ShapeShared> = observableListOf()

    suspend fun getWorldData(): WorldShared {
        val newWorldData = bouncyService.getWorldData()
        worldData.value = newWorldData
        return newWorldData
    }

    suspend fun getClients(): List<GameClientShared> {
        val newAddresses = bouncyService.getClients()
        clients.syncWithList(newAddresses)
        return clients
    }

    suspend fun getShapes(): List<ShapeShared> {
        val newShapes = bouncyService.getShapes()
        shapes.syncWithList(newShapes)
        return newShapes
    }

    suspend fun connectToServer() {
        while (true) {
            try {
                wsBouncyService.socketConnection { _, input ->
                    coroutineScope {
                        launch {
                            while (true) {
                                for (newWorldData in input) {
                                    worldData.value = newWorldData
                                }
                                delay(500)
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                console.error("connection to server closed, or other exception (${ex.message}). Restarting in 5s")
                delay(5000)
            }
        }
    }
}

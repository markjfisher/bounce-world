package bw

import io.kvision.remote.getService
import io.kvision.state.ObservableList
import io.kvision.state.ObservableValue
import io.kvision.state.observableListOf
import io.kvision.utils.syncWithList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Model {
    private val bouncyService = BouncyService()
    private val wsBouncyService = getService<IBouncyWsService>()
    private val bwChannel = Channel<Int>()

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

    fun connectToServer() {
        AppScope.launch {
            println("Connecting to server...")
            while (true) {
                wsBouncyService.socketConnection { output, input ->
                    coroutineScope {
                        launch {
                            for (i in bwChannel) {
                                println("could send worldData: $i")
                                output.send(i)
                            }
                        }
                        launch {
                            for (newWorldData in input) {
                                worldData.value = newWorldData
                            }
                        }
                    }
                }
                delay(2000)
            }
        }
    }
}
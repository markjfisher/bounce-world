package bw

import io.kvision.remote.getService
import io.kvision.state.ObservableList
import io.kvision.state.ObservableValue
import io.kvision.state.observableListOf
import io.kvision.utils.syncWithList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Model {
    private val bouncyService = BouncyService()
    private val wsBouncyService = getService<IBouncyWsService>()
//    private val bwChannel = Channel<Int>()

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
        // if the connection closes (e.g. server restarted), this will ensure we retry
        while (true) {
            try {
                wsBouncyService.socketConnection { _, input ->
                    coroutineScope {
        //                    launch {
        //                        // SEND SIDE - nothing uses this yet, could use it for updates, but there's a full REST interface the client can use
        //                        while (true) {
        //                            for (i in bwChannel) {
        //                                println("sending: $i")
        //                                output.send(i)
        //                            }
        //                            delay(200)
        //                        }
        //                    }
                        launch {
                            // RECEIVE SIDE - gets updates on world state directly from server
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
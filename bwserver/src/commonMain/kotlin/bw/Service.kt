package bw

import io.kvision.annotations.KVService
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

@KVService
interface IBouncyService {
    suspend fun getWorldData(): WorldShared
    suspend fun getClients(): List<GameClientShared>
    suspend fun getShapes(): List<ShapeShared>
}

@KVService
interface IBouncyWsService {
    suspend fun socketConnection(input: ReceiveChannel<Int>, output: SendChannel<WorldShared>) {}
    suspend fun socketConnection(handler: suspend (SendChannel<Int>, ReceiveChannel<WorldShared>) -> Unit) {}
}
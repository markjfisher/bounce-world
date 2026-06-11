package bw

import dev.kilua.rpc.annotations.RpcService
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

@RpcService
interface IBouncyService {
    suspend fun getWorldData(): WorldShared
    suspend fun getClients(): List<GameClientShared>
    suspend fun getShapes(): List<ShapeShared>
}

@RpcService
interface IBouncyWsService {
    suspend fun socketConnection(input: ReceiveChannel<Int>, output: SendChannel<WorldShared>) {}
    suspend fun socketConnection(handler: suspend (SendChannel<Int>, ReceiveChannel<WorldShared>) -> Unit) {}
}

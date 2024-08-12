package controller

import domain.DelayInfo
import domain.World
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.MediaType.TEXT_PLAIN
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.reactivex.Flowable
import jakarta.validation.Valid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.rx2.asFlowable

@Controller("/") // bounce world, keep strings short for the 8 bit memory
open class WorldController(private val world: World) {
    private val channelsContext = CoroutineScope(Dispatchers.Default).coroutineContext

    @Get("w/{clientId}", produces = [MediaType.APPLICATION_OCTET_STREAM])
    fun getWorldData(clientId: String): HttpResponse<Any> {
        val client = world.getClient(clientId) ?: return HttpResponse.notFound()

        val channel = world.registerClientChannel(clientId)
        val flowable: Flowable<ByteArray> = channel.consumeAsFlow().asFlowable(channelsContext)
        println("creating data connection for ${client.name} ($clientId)")
        return HttpResponse.ok(flowable)
    }

    @Get("hb/{clientId}")
    fun heartbeat(clientId: String): HttpResponse<String> {
        val client = world.getClient(clientId)
        if (client != null) {
            println("heartbeat from ${client.name} ($clientId)")
            world.clientHeartbeats[clientId] = System.currentTimeMillis()
        }
        return HttpResponse.ok()
    }

    @Post("config/delay", produces = [TEXT_PLAIN], consumes = [APPLICATION_JSON])
    open fun setConfigDelay(@Valid @Body delayInfo: DelayInfo): HttpResponse<String> {
        world.setDelay(delayInfo.delay)
        return HttpResponse.ok("configured delay to ${delayInfo.delay} milliseconds")
    }

}
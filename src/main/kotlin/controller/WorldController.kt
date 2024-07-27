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

@Controller("/world")
open class WorldController(private val world: World) {
    private val channelsContext = CoroutineScope(Dispatchers.Default).coroutineContext

    @Get("/stream/{clientId}", produces = [MediaType.APPLICATION_OCTET_STREAM])
    fun flowRaw(clientId: String): HttpResponse<Any> {
        world.getClient(clientId) ?: return HttpResponse.notFound()

        val channel = world.registerClientChannel(clientId)
        val flowable: Flowable<ByteArray> = channel.consumeAsFlow().asFlowable(channelsContext)
        return HttpResponse.ok(flowable)
    }

    @Get("/heartbeat/{clientId}")
    fun heartbeat(clientId: String): HttpResponse<String> {
        if (world.getClient(clientId) != null) {
            world.clientHeartbeats[clientId] = System.currentTimeMillis()
        }
        return HttpResponse.ok()
    }

    @Post("/config/delay", produces = [TEXT_PLAIN], consumes = [APPLICATION_JSON])
    open fun setConfigDelay(@Valid @Body delayInfo: DelayInfo): HttpResponse<String> {
        world.setDelay(delayInfo.delay)
        return HttpResponse.ok("configured delay to ${delayInfo.delay} milliseconds")
    }

}
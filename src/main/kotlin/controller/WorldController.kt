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

//    fun flowRaw(clientId: String): Flowable<ByteArray> {
//        val channel = world.registerClientChannel(clientId)
//        return channel.consumeAsFlow().asFlowable(channelsContext)
//    }

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

//    @Get("/flow/{clientId}", produces = [MediaType.APPLICATION_OCTET_STREAM])
//    fun flowTest(clientId: String): Flowable<ByteArray> = flow {
//        if (world.getClient(clientId) == null) { return@flow }
//
//        while (true) {
//            val csv = world.asCSV(clientId)
//            if (csv.isNotEmpty()) {
//                println("emitting $csv")
//                val data = csv.split(",").map { it.toInt().toByte() }.toByteArray()
//                emit(data)
//            }
//
//            delay(1000) // Adjust the pause duration as needed
//        }
//    }.asFlowable()

//    fun flowRaw(clientId: String): Flowable<ByteArray> {
//        return Flowable.create({ emitter: FlowableEmitter<ByteArray> ->
//            try {
//                while (!emitter.isCancelled) {
//                    val data = asCSV(clientId).split(",").map { it.toInt().toByte() }.toByteArray()
//                    emitter.onNext(data)
//
//                    // Pause for a specified time
//                    try {
//                        TimeUnit.MILLISECONDS.sleep(1000) // Adjust the pause duration as needed
//                    } catch (e: InterruptedException) {
//                        emitter.onError(e) // Handle interruption appropriately
//                    }
//                }
//                emitter.onComplete() // Complete the stream if you have a terminating condition
//            } catch (e: Exception) {
//                emitter.onError(e) // Handle any exceptions
//            }
//        }, BackpressureStrategy.BUFFER)
//    }

//    @Get("/flow/{clientId}", produces = [MediaType.APPLICATION_OCTET_STREAM])
//    @SingleResult
//    fun flowRawSingle(clientId: String): Flowable<ByteArray> {
//        return Flowable.fromCallable { asCSV(clientId).split(",").map { it.toInt().toByte() }.toByteArray() }
//    }
//
//    @Get("/txt/{clientId}", produces = [MediaType.TEXT_PLAIN])
//    fun data(clientId: String): HttpResponse<String> {
//        return HttpResponse.ok(asCSV(clientId))
//    }
//
//    @Get("/raw/{clientId}", produces = [MediaType.APPLICATION_OCTET_STREAM])
//    fun dataBytes(clientId: String): HttpResponse<ByteArray> {
//        return HttpResponse.ok(asCSV(clientId).split(",").map { it.toInt().toByte() }.toByteArray())
//    }

}
package controller

import domain.World
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MediaType.APPLICATION_OCTET_STREAM
import io.micronaut.http.MediaType.TEXT_PLAIN
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@Controller("/shapes")
@ExecuteOn(TaskExecutors.BLOCKING)
open class ShapesController(private val world: World) {

    @Get("data", produces = [TEXT_PLAIN])
    open fun getShapeData(): ByteArray = world.shapes.fold(mutableListOf<Byte>()) { acc, s ->
        acc.apply {
            add(s.id.toByte())
            add(s.sideLength.toByte())
            addAll(s.codedString().toByteArray().toList())
        }
    }.toByteArray()

    @Get("count", produces = [APPLICATION_OCTET_STREAM])
    open fun getCount(): HttpResponse<ByteArray> {
        // allow the client to pre-calculate things by knowing number of shapes ahead of reading data
        // return as a byte so the client doesn't need to convert ascii number to an int
//        println("returning count as ${world.shapes.count()}")
        return HttpResponse.ok(byteArrayOf(world.shapes.count().toByte()))
    }

}
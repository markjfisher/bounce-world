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
    open fun getShapeData(): String {
        // a simple parsable string using comma separators for client to convert to Shapes data.
        return world.shapes.joinToString(",") { s -> "${s.id},${s.sideLength},${s.codedString()}" }
    }

    @Get("count", produces = [APPLICATION_OCTET_STREAM])
    open fun getCount(): HttpResponse<ByteArray> {
        // allow the client to pre-calculate things by knowing number of shapes ahead of reading data
        // return as a byte so the client doesn't need to convert ascii number to an int
        println("returning count as ${world.shapes.count()}")
        return HttpResponse.ok(byteArrayOf(world.shapes.count().toByte()))
    }

}
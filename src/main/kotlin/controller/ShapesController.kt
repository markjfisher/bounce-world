package controller

import domain.World
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

    @Get("count", produces = [TEXT_PLAIN])
    open fun getCount(): Int {
        // allow the client to pre-calculate things by knowing number of shapes ahead of reading data
        return world.shapes.count()
    }

}
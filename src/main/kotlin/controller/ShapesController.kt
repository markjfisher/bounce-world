package controller

import domain.Shape
import domain.ShapeInfo
import domain.World
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@Controller("/shapes")
@ExecuteOn(TaskExecutors.BLOCKING)
open class ShapesController(private val world: World) {
    @Get(produces = [MediaType.APPLICATION_JSON])
    open fun getShapes(): HttpResponse<ShapeInfo> {
//        return HttpResponse.created(world.simulator.shapes.values.toList())
//        return HttpResponse.created(
//            world.simulator.shapes.values.map { s -> ShapeInfo("${s.id},${s.sideLength},${s.codedString()}")}
//        )
        return HttpResponse.created(
            ShapeInfo(
                world.simulator.shapes.values.joinToString(",") { s -> "${s.id},${s.sideLength},${s.codedString()}" }
            )
        )
    }



    @Get("data", produces = [MediaType.TEXT_PLAIN])
    open fun getShapeData(): String {
        return world.simulator.shapes.values.joinToString(",") { s -> "${s.id},${s.sideLength},${s.codedString()}" }
    }

    @Get("count", produces = [MediaType.TEXT_PLAIN])
    open fun getCount(): Int {
        return world.simulator.shapes.count()
    }

}
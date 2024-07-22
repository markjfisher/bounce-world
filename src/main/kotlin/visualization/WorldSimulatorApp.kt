package visualization

import domain.World
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import tornadofx.*
import kotlinx.coroutines.*

class WorldSimulatorApp: App(WorldView::class)

class WorldView : View("World Simulator") {
    private val world = World()
    private val f = 1200.0 / 160.0
    private val canvas = Canvas(1200.0, 600.0)

    init {
        updateWorld()
    }

    override val root = borderpane {
        center {
            add(canvas)
        }
    }

    private fun drawWorld() {
        val gc = canvas.graphicsContext2D
        with(gc) {
            clearRect(0.0, 0.0, canvas.width, canvas.height)
            world.simulator.bodies.forEach { body ->
                val radius = world.simulator.shapes[body.shapeId]!!.sideLength / 2f * 4f
                val screenX = body.position.x - radius
                val screenY = body.position.y - radius
                fill = Color.RED
                fillOval(screenX * f, screenY.toDouble() * f, radius * 2.0 * f, radius * 2.0 * f)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateWorld() {
        GlobalScope.launch {
            while (true) {
                delay(10)
                world.simulator.step()

                runLater {
                    drawWorld()
                }
            }
        }
    }
}

fun main() {
    launch<WorldSimulatorApp>()
}
package visualization

import config.WorldConfiguration
import domain.World
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import simulator.WorldSimulator
import tornadofx.App
import tornadofx.View
import tornadofx.borderpane
import tornadofx.center
import tornadofx.launch
import tornadofx.runLater
import kotlin.random.Random

class WorldSimulatorApp: App(WorldView::class)

class WorldView : View("World Simulator") {
    private val worldConfig = WorldConfiguration().apply { shouldAutoStart = false }
    private val simulator = WorldSimulator(worldConfig)
    private val world = World(worldConfig, simulator)
    private val f = 1200.0 / worldConfig.width
    private val canvas = Canvas(1200.0, 600.0)
    private val colours = mutableMapOf<Int, Color>()

    private val simulationScope = CoroutineScope(Dispatchers.Default)

    init {
        val newBodies = world.createBodies(0,0, 0, List(4) { 5 } + List(10) { 3 } + List(8) { 1 })
        world.simulator.addBodies(newBodies)
        colours.putAll(world.simulator.bodies().associate {
            it.id to Color.color(
                Random.nextDouble(),
                Random.nextDouble(),
                Random.nextDouble()
            )
        })
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
            clearRect(0.0, 0.0, canvas.width, canvas.height) // Clear the canvas
            world.simulator.bodies().forEach { body ->
                val radius = body.radius * 4f // scaled to world size
                val positions = calculateWrappedPositions(body.position.x.toDouble(), body.position.y.toDouble(), radius.toDouble(), world.simulator.width().toDouble(), world.simulator.height().toDouble())

                // Draw the circle representing the body at potentially wrapped positions
                fill = colours[body.id]!!
                positions.forEach { (x, y) ->
                    // scale up to canvas size from world size via "f"
                    // need to subtract radius because ovals don't draw from the centre, but from the top left corner
                    val xp = (x - radius) * f
                    val yp = (y - radius) * f
                    val w = radius * 2.0 * f
                    val h = radius * 2.0 * f
                    // println("[${positions.size}] body: $body, x: $x, y: $y, xp: $xp, yp: $yp")
                    fillOval(xp, yp, w, h)
                }
            }
        }
    }

    private fun calculateWrappedPositions(x: Double, y: Double, radius: Double, width: Double, height: Double): List<Pair<Double, Double>> {
        val positions = mutableListOf<Pair<Double, Double>>()

        // Original position
        positions.add(Pair(x, y))

        // Check and add wrapped positions
        val leftEdge = x - radius < 0
        val rightEdge = x + radius > width
        val topEdge = y - radius < 0
        val bottomEdge = y + radius > height

        if (leftEdge) positions.add(Pair(x + width, y))
        if (rightEdge) positions.add(Pair(x - width, y))
        if (topEdge) positions.add(Pair(x, y + height))
        if (bottomEdge) positions.add(Pair(x, y - height))

        // Handle corners
        if (leftEdge && topEdge) positions.add(Pair(x + width, y + height))
        if (rightEdge && topEdge) positions.add(Pair(x - width, y + height))
        if (leftEdge && bottomEdge) positions.add(Pair(x + width, y - height))
        if (rightEdge && bottomEdge) positions.add(Pair(x - width, y - height))

        return positions
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateWorld() {
        simulationScope.launch {
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
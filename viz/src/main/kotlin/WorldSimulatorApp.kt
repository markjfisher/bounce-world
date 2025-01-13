package visualization

import config.WorldConfig
import domain.World
import geometry.Point
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import simulator.BoundedWorldSimulator
import simulator.WrappingWorldSimulator
import tornadofx.App
import tornadofx.View
import tornadofx.borderpane
import tornadofx.center
import tornadofx.launch
import tornadofx.runLater
import kotlin.random.Random

class WorldSimulatorApp: App(WorldView::class)

class WorldView : View("World Simulator") {
    private val worldConfig = WorldConfig(
        width = 80,
        height = 80,
        updatesPerSecond = 160,
        shouldAutoStart = false,
        initialSpeed = 12f,
        heartbeatTimeoutMillis = 40000,
        locationPattern = "grid",
        enableWrapping = false,
        tcpHost = "0.0.0.0",
        tcpPort = 9002,
    )
    private val worldSimulator = WrappingWorldSimulator(worldConfig)
    private val boundedWorldSimulator = BoundedWorldSimulator(worldConfig)
    private val world = World(worldConfig, worldSimulator, boundedWorldSimulator)
    private val canvasWidth = 800.0
    private val canvasHeight = 800.0
    private val f = canvasWidth / worldConfig.width
    private val canvas = Canvas(canvasWidth, canvasHeight)
    private val colours = mutableMapOf<Int, Color>()

    private val simulationScope = CoroutineScope(Dispatchers.Default)

    init {
//        val sizes = List(50) { 5 } + List(300) { 4 } + List(500) { 3 } + List(600) { 1 }
        val sizes = List(2) { 5 } + List(3) { 4 } + List(8) { 3 } + List(15) { 2 } + List(20) { 1 }
        val grouped = world.shapes.groupBy { it.sideLength }

        sizes.forEach { size ->
            val shape = grouped[size]!!.random()
            val body = world.createBody(shape.id, Point(Random.nextInt(world.getWorldWidth() - 20) + shape.sideLength + 10, Random.nextInt(world.getWorldHeight() - 20) + shape.sideLength + 10))
            world.currentSimulator.bodies.add(body)
        }

        colours.putAll(world.currentSimulator.bodies.associate {
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
            world.currentSimulator.bodies.forEach { body ->
                val radius = body.radius
                val positions = calculateWrappedPositions(body.position.x.toDouble(), body.position.y.toDouble(), radius.toDouble(), world.currentSimulator.width.toDouble(), world.currentSimulator.height.toDouble())

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

    private fun updateWorld() {
        simulationScope.launch {
            while (true) {
                delay(2)
                world.currentSimulator.step()

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
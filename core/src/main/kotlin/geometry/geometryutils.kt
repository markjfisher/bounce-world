package geometry

import org.joml.Vector2f
import kotlin.math.cos
import kotlin.math.sin

// Translates the point given to the coordinate system of the target, rotating so that the result is relative to the target
// with a direction pointing north.
// This means we can always consider the target's direction as North and get the point's relative position to this view,
// no matter what the real direction the target is going in.
fun translatePointToTargetViewCoordinates(target: Vector2f, direction: Double, point: Vector2f): Vector2f {
    // Translate the point to be relative to the centre's position
    val translatedX = point.x - target.x
    val translatedY = point.y - target.y

    // Correctly rotate the translated point to align the centre's forward direction with the "up" direction in the view
    val adjustedDir = -(direction - Math.PI / 2.0)
    val rotatedX = translatedX * cos(adjustedDir) - translatedY * sin(adjustedDir)
    val rotatedY = translatedX * sin(adjustedDir) + translatedY * cos(adjustedDir)

    return Vector2f(rotatedX.toFloat(), rotatedY.toFloat())
}
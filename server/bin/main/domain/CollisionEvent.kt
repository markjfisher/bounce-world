package domain

data class CollisionEvent(
    val timeUntilCollision: Float, // Time until the collision occurs
    val body1: Body, // The first body involved in the collision
    val body2: Body? = null // The second body involved in the collision, null if it's a wall collision
)
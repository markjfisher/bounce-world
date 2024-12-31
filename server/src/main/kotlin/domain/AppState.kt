package domain

// This isn't used yet, it's part of an idea to have states generated so they can be done ahead of time and have multiple sent to client
// so it can queue them up.
data class AppState(
    val bodies: List<Body>,
    val collisions: Set<Int>,
    val step: Int
) {
    fun copy(): AppState {
        return AppState(bodies.map { it.copy() }, collisions.toSet(), step)
    }
}

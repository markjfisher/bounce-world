package domain

data class AppState(
    val bodies: List<Body>,
    val collisions: Set<Int>,
    val step: Int
) {
    fun copy(): AppState {
        return AppState(bodies.map { it.copy() }, collisions.toSet(), step)
    }
}

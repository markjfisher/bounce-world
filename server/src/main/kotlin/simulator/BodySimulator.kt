package simulator

import domain.Body

interface BodySimulator {
    fun step()
    fun addBodies(bodies: List<Body>)
    fun bodies(): MutableList<Body>
    fun setWidth(width: Int)
    fun setHeight(height: Int)
    fun width(): Int
    fun height(): Int
    fun collisions(): Set<Int>
    fun currentStep(): Int
    fun isWrapping(): Boolean
    fun reset()
}

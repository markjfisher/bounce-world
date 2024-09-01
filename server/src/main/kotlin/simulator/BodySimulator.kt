package simulator

import domain.Body

interface BodySimulator {
    var width: Int
    var height: Int
    var currentStep: Int
    val collisions: MutableSet<Int>
    val bodies: MutableList<Body>

    fun step()
    fun addBodies(bodies: List<Body>)
    fun reset()
}

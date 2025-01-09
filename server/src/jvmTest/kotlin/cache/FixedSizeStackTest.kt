package cache

import domain.AppState
import domain.Body
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.joml.Vector2f

class FixedSizeStackTest : StringSpec({
    val body1 = Body(id = 1, position = Vector2f(), velocity = Vector2f(), mass = 1f, radius = 1f, shapeId = 1)
    val body2 = Body(id = 2, position = Vector2f(), velocity = Vector2f(), mass = 1f, radius = 1f, shapeId = 2)
    val body3 = Body(id = 3, position = Vector2f(), velocity = Vector2f(), mass = 1f, radius = 1f, shapeId = 3)
    val appState1 = AppState(bodies = listOf(body1), collisions = setOf(0), step = 1)
    val appState2 = AppState(bodies = listOf(body2), collisions = setOf(1), step = 2)
    val appState3 = AppState(bodies = listOf(body3), collisions = setOf(1), step = 3)

    "push should add an element to the stack" {
        val stack = FixedSizeStack<AppState>(2)
        stack.push(appState1)
        stack.size() shouldBe 1
    }

    "push should remove the oldest element when maxSize is exceeded" {
        val stack = FixedSizeStack<AppState>(2)
        stack.push(appState1)
        stack.push(appState2)
        stack.push(appState3) // This should remove appState1
        stack.size() shouldBe 2
        stack.peek() shouldBe appState2
    }

    "peek should return the last element without removing it" {
        val stack = FixedSizeStack<AppState>(2)
        stack.push(appState1)
        stack.peek() shouldBe appState1
        stack.size() shouldBe 1 // Size should remain the same
    }

    "pop should remove and return the last element" {
        val stack = FixedSizeStack<AppState>(2)
        stack.push(appState1)
        stack.pop() shouldBe appState1
        stack.size() shouldBe 0
    }

    "size should return the correct number of elements in the stack" {
        val stack = FixedSizeStack<AppState>(3)
        stack.push(appState1)
        stack.push(appState2)
        stack.size() shouldBe 2
    }

    "clear should remove all elements from the stack" {
        val stack = FixedSizeStack<AppState>(3)
        stack.push(appState1)
        stack.push(appState2)
        stack.clear()
        stack.size() shouldBe 0
    }
})

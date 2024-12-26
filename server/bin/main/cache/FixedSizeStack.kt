package cache

import domain.AppState
import java.util.LinkedList

// Adds to the front, and reads from the back, keeping maxSize entries.
class FixedSizeStack(private val maxSize: Int) {
    private val stack: LinkedList<AppState> = LinkedList()

    fun push(appState: AppState) {
        if (stack.size == maxSize) {
            stack.removeLast()
        }
        stack.addFirst(appState)
    }

    fun peek(): AppState? = stack.lastOrNull()
    fun pop(): AppState? = stack.removeLastOrNull()

    fun size(): Int = stack.size

    fun clear() {
        stack.clear()
    }
}
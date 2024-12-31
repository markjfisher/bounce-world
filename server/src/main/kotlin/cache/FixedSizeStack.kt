package cache

import java.util.LinkedList

// Adds to the front, and reads from the back, keeping maxSize entries.
class FixedSizeStack<T>(private val maxSize: Int) {
    private val stack: LinkedList<T> = LinkedList()

    fun push(item: T) {
        if (stack.size == maxSize) {
            stack.removeLast()
        }
        stack.addFirst(item)
    }

    fun peek(): T? = stack.lastOrNull()
    fun pop(): T? = stack.removeLastOrNull()

    fun size(): Int = stack.size

    fun clear() {
        stack.clear()
    }
}
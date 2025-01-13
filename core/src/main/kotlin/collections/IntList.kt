package collections

// Used primarily by Quadtree
class IntList(private val numFields: Int) {
    var data: IntArray
    private var num = 0
    private var cap = 128
    private var freeElement = -1

    // Creates a new list of elements which each consist of integer fields.
    // 'numFields' specifies the number of integer fields each element has.
    init {
        data = IntArray(cap * numFields)
    }

    // Returns the number of elements in the list.
    fun size(): Int {
        return num
    }

    // Returns the value of the specified field for the nth element.
    fun get(n: Int, field: Int): Int {
        assert(n in 0..<num && field in 0..<numFields)
        return data[n * numFields + field]
    }

    // Sets the value of the specified field for the nth element.
    fun set(n: Int, field: Int, value: Int) {
        assert(n in 0..<num && field in 0..<numFields)
        data[n * numFields + field] = value
    }

    // Clears the list, making it empty.
    fun clear() {
        num = 0
        freeElement = -1
    }

    // Inserts an element to the back of the list and returns an index to it.
    fun pushBack(): Int {
        val newPos = (num + 1) * numFields

        // If the list is full, we need to reallocate the buffer to make room
        // for the new element.
        if (newPos > cap) {
            // Use double the size for the new capacity.
            val newCap = newPos * 2

            // Allocate new array and copy former contents.
            val newArray = IntArray(newCap)
            System.arraycopy(data, 0, newArray, 0, cap)
            data = newArray

            // Set the old capacity to the new capacity.
            cap = newCap
        }
        return num++
    }

    // Removes the element at the back of the list.
    fun popBack() {
        // Just decrement the list size.
        assert(num > 0)
        --num
    }

    // Inserts an element to a vacant position in the list and returns an index to it.
    fun insert(): Int {
        // If there's a free index in the free list, pop that and use it.
        if (freeElement != -1) {
            val index = freeElement
            val pos = index * numFields

            // Set the free index to the next free index.
            freeElement = data[pos]

            // Return the free index.
            return index
        }
        // Otherwise insert to the back of the array.
        return pushBack()
    }

    // Removes the nth element in the list.
    fun erase(n: Int) {
        // Push the element to the free list.
        val pos = n * numFields
        data[pos] = freeElement
        freeElement = n
    }

    fun toSet(): Set<Int> {
        return (0 until num).fold(mutableSetOf()) { set, id ->
            set.add(get(id, 0))
            set
        }
    }
}
package data

import collections.IntList

// From https://stackoverflow.com/questions/41946007/efficient-and-well-explained-implementation-of-a-quadtree-for-2d-collision-det

interface IQtVisitor {
    // Called when traversing a branch node.
    // (mx, my) indicate the center of the node's AABB.
    // (sx, sy) indicate the half-size of the node's AABB.
    fun branch(qt: Quadtree, node: Int, depth: Int, mx: Int, my: Int, sx: Int, sy: Int)

    // Called when traversing a leaf node.
    // (mx, my) indicate the center of the node's AABB.
    // (sx, sy) indicate the half-size of the node's AABB.
    fun leaf(qt: Quadtree, node: Int, depth: Int, mx: Int, my: Int, sx: Int, sy: Int)
}

class Quadtree(width: Int, height: Int,
    // Maximum allowed elements in a leaf before the leaf is subdivided/split unless the leaf is at the maximum allowed tree depth.
    private val maxElements: Int,
    // Stores the maximum depth allowed for the quadtree.
    private val maxDepth: Int
) {
    fun getId(element: Int): Int = elts.get(element, ELT_IDX_ID)

    fun insert(id: Int, x1: Float, y1: Float, x2: Float, y2: Float): Int {
        // Insert a new element.
        val newElement: Int = elts.insert()

        // Set the fields of the new element.
        elts.set(newElement, ELT_IDX_LFT, floorInt(x1))
        elts.set(newElement, ELT_IDX_TOP, floorInt(y1))
        elts.set(newElement, ELT_IDX_RGT, floorInt(x2))
        elts.set(newElement, ELT_IDX_BTM, floorInt(y2))
        elts.set(newElement, ELT_IDX_ID, id)

        // Insert the element to the appropriate leaf node(s).
        nodeInsert(0, 0, rootMx, rootMy, rootSx, rootSy, newElement)
        return newElement
    }

    // Removes the specified element from the tree.
    fun remove(element: Int) {
        // Find the leaves.
        val lft: Int = elts.get(element, ELT_IDX_LFT)
        val top: Int = elts.get(element, ELT_IDX_TOP)
        val rgt: Int = elts.get(element, ELT_IDX_RGT)
        val btm: Int = elts.get(element, ELT_IDX_BTM)
        val leaves: IntList = findLeaves(0, 0, rootMx, rootMy, rootSx, rootSy, lft, top, rgt, btm)

        // For each leaf node, remove the element node.
        for (j in 0 until leaves.size()) {
            val ndIndex: Int = leaves.get(j, ND_IDX_INDEX)

            // Walk the list until we find the element node.
            var nodeIndex: Int = nodes.get(ndIndex, NODE_IDX_FC)
            var prevIndex = -1
            while (nodeIndex != -1 && enodes.get(nodeIndex, ENODE_IDX_ELT) != element) {
                prevIndex = nodeIndex
                nodeIndex = enodes.get(nodeIndex, ENODE_IDX_NEXT)
            }

            if (nodeIndex != -1) {
                // Remove the element node.
                val nextIndex: Int = enodes.get(nodeIndex, ENODE_IDX_NEXT)
                if (prevIndex == -1) nodes.set(ndIndex, NODE_IDX_FC, nextIndex)
                else enodes.set(prevIndex, ENODE_IDX_NEXT, nextIndex)
                enodes.erase(nodeIndex)

                // Decrement the leaf element count.
                nodes.set(ndIndex, NODE_IDX_NUM, nodes.get(ndIndex, NODE_IDX_NUM) - 1)
            }
        }

        // Remove the element.
        elts.erase(element)
    }

    // Cleans up the tree, removing empty leaves.
    fun cleanup() {
        val toProcess: IntList = IntList(1)

        // Only process the root if it's not a leaf.
        if (nodes.get(0, NODE_IDX_NUM) == -1) {
            // Push the root index to the stack.
            toProcess.set(toProcess.pushBack(), 0, 0)
        }

        while (toProcess.size() > 0) {
            // Pop a node from the stack.
            val node: Int = toProcess.get(toProcess.size() - 1, 0)
            val fc: Int = nodes.get(node, NODE_IDX_FC)
            var numEmptyLeaves = 0
            toProcess.popBack()

            // Loop through the children.
            for (j in 0..3) {
                val child = fc + j

                // Increment empty leaf count if the child is an empty 
                // leaf. Otherwise if the child is a branch, add it to
                // the stack to be processed in the next iteration.
                if (nodes.get(child, NODE_IDX_NUM) == 0) ++numEmptyLeaves
                else if (nodes.get(child, NODE_IDX_NUM) == -1) {
                    // Push the child index to the stack.
                    toProcess.set(toProcess.pushBack(), 0, child)
                }
            }

            // If all the children were empty leaves, remove them and 
            // make this node the new empty leaf.
            if (numEmptyLeaves == 4) {
                // Remove all 4 children in reverse order so that they 
                // can be reclaimed on subsequent insertions in proper
                // order.
                nodes.erase(fc + 3)
                nodes.erase(fc + 2)
                nodes.erase(fc + 1)
                nodes.erase(fc + 0)

                // Make this node the new empty leaf.
                nodes.set(node, NODE_IDX_FC, -1)
                nodes.set(node, NODE_IDX_NUM, 0)
            }
        }
    }

    // Returns the list of Pairs of (element index, Original ID of element) so we don't have to maintain a separate mapping of element index values to IDs we already supplied.
    // This is the only way to get the ELT_IDX_ID values back out of the quad tree, and wasn't originally part of the implementation.
    fun queryWithIds(x1: Float, y1: Float, x2: Float, y2: Float, omitElement: Int = -1): List<Pair<Int, Int>> =
        query(x1, y1, x2, y2, omitElement).let { ids ->
            (0 until ids.size()).fold(mutableListOf()) { ac, elementId ->
                val element = ids.get(elementId, 0)
                ac.add(Pair(element, elts.get(element, ELT_IDX_ID)))
                ac
            }
        }

    // Returns a list of elements found in the specified rectangle excluding the specified element to omit.
    fun query(x1: Float, y1: Float, x2: Float, y2: Float, omitElement: Int = -1): IntList {
        val out: IntList = IntList(1)

        // Find the leaves that intersect the specified query rectangle.
        val qlft = floorInt(x1)
        val qtop = floorInt(y1)
        val qrgt = floorInt(x2)
        val qbtm = floorInt(y2)
        val leaves: IntList = findLeaves(0, 0, rootMx, rootMy, rootSx, rootSy, qlft, qtop, qrgt, qbtm)

        if (tempSize < elts.size()) {
            tempSize = elts.size()
            temp = BooleanArray(tempSize)
        }

        // For each leaf node, look for elements that intersect.
        for (j in 0 until leaves.size()) {
            val ndIndex: Int = leaves.get(j, ND_IDX_INDEX)

            // Walk the list and add elements that intersect.
            var eltNodeIndex: Int = nodes.get(ndIndex, NODE_IDX_FC)
            while (eltNodeIndex != -1) {
                val element: Int = enodes.get(eltNodeIndex, ENODE_IDX_ELT)
                val lft: Int = elts.get(element, ELT_IDX_LFT)
                val top: Int = elts.get(element, ELT_IDX_TOP)
                val rgt: Int = elts.get(element, ELT_IDX_RGT)
                val btm: Int = elts.get(element, ELT_IDX_BTM)
                if (!temp[element] && element != omitElement && intersect(qlft, qtop, qrgt, qbtm, lft, top, rgt, btm)) {
                    out.set(out.pushBack(), 0, element)
                    temp[element] = true
                }
                eltNodeIndex = enodes.get(eltNodeIndex, ENODE_IDX_NEXT)
            }
        }

        // Unmark the elements that were inserted.
        for (j in 0 until out.size()) temp[out.get(j, 0)] = false
        return out
    }

    // Traverses all the nodes in the tree, calling 'branch' for branch nodes and 'leaf' 
    // for leaf nodes.
    fun traverse(visitor: IQtVisitor) {
        val toProcess: IntList = IntList(ND_NUM)
        pushNode(toProcess, 0, 0, rootMx, rootMy, rootSx, rootSy)

        while (toProcess.size() > 0) {
            val backIdx: Int = toProcess.size() - 1
            val ndMx: Int = toProcess.get(backIdx, ND_IDX_MX)
            val ndMy: Int = toProcess.get(backIdx, ND_IDX_MY)
            val ndSx: Int = toProcess.get(backIdx, ND_IDX_SX)
            val ndSy: Int = toProcess.get(backIdx, ND_IDX_SY)
            val ndIndex: Int = toProcess.get(backIdx, ND_IDX_INDEX)
            val ndDepth: Int = toProcess.get(backIdx, ND_IDX_DEPTH)
            val fc: Int = nodes.get(ndIndex, NODE_IDX_FC)
            toProcess.popBack()

            if (nodes.get(ndIndex, NODE_IDX_NUM) == -1) {
                // Push the children of the branch to the stack.
                val hx = ndSx shr 1
                val hy = ndSy shr 1
                val l = ndMx - hx
                val t = ndMy - hx
                val r = ndMx + hx
                val b = ndMy + hy
                pushNode(toProcess, fc + 0, ndDepth + 1, l, t, hx, hy)
                pushNode(toProcess, fc + 1, ndDepth + 1, r, t, hx, hy)
                pushNode(toProcess, fc + 2, ndDepth + 1, l, b, hx, hy)
                pushNode(toProcess, fc + 3, ndDepth + 1, r, b, hx, hy)
                visitor.branch(this, ndIndex, ndDepth, ndMx, ndMy, ndSx, ndSy)
            } else visitor.leaf(this, ndIndex, ndDepth, ndMx, ndMy, ndSx, ndSy)
        }
    }

    private fun findLeaves(
        node: Int, depth: Int,
        mx: Int, my: Int, sx: Int, sy: Int,
        lft: Int, top: Int, rgt: Int, btm: Int
    ): IntList {
        val leaves: IntList = IntList(ND_NUM)
        val toProcess: IntList = IntList(ND_NUM)
        pushNode(toProcess, node, depth, mx, my, sx, sy)

        while (toProcess.size() > 0) {
            val backIdx: Int = toProcess.size() - 1
            val ndMx: Int = toProcess.get(backIdx, ND_IDX_MX)
            val ndMy: Int = toProcess.get(backIdx, ND_IDX_MY)
            val ndSx: Int = toProcess.get(backIdx, ND_IDX_SX)
            val ndSy: Int = toProcess.get(backIdx, ND_IDX_SY)
            val ndIndex: Int = toProcess.get(backIdx, ND_IDX_INDEX)
            val ndDepth: Int = toProcess.get(backIdx, ND_IDX_DEPTH)
            toProcess.popBack()

            // If this node is a leaf, insert it to the list.
            if (nodes.get(ndIndex, NODE_IDX_NUM) != -1) pushNode(leaves, ndIndex, ndDepth, ndMx, ndMy, ndSx, ndSy)
            else {
                // Otherwise push the children that intersect the rectangle.
                val fc: Int = nodes.get(ndIndex, NODE_IDX_FC)
                val hx = ndSx / 2
                val hy = ndSy / 2
                val l = ndMx - hx
                val t = ndMy - hx
                val r = ndMx + hx
                val b = ndMy + hy

                if (top <= ndMy) {
                    if (lft <= ndMx) pushNode(toProcess, fc + 0, ndDepth + 1, l, t, hx, hy)
                    if (rgt > ndMx) pushNode(toProcess, fc + 1, ndDepth + 1, r, t, hx, hy)
                }
                if (btm > ndMy) {
                    if (lft <= ndMx) pushNode(toProcess, fc + 2, ndDepth + 1, l, b, hx, hy)
                    if (rgt > ndMx) pushNode(toProcess, fc + 3, ndDepth + 1, r, b, hx, hy)
                }
            }
        }
        return leaves
    }

    private fun nodeInsert(index: Int, depth: Int, mx: Int, my: Int, sx: Int, sy: Int, element: Int) {
        // Find the leaves and insert the element to all the leaves found.
        val lft: Int = elts.get(element, ELT_IDX_LFT)
        val top: Int = elts.get(element, ELT_IDX_TOP)
        val rgt: Int = elts.get(element, ELT_IDX_RGT)
        val btm: Int = elts.get(element, ELT_IDX_BTM)
        val leaves: IntList = findLeaves(index, depth, mx, my, sx, sy, lft, top, rgt, btm)

        for (j in 0 until leaves.size()) {
            val ndMx: Int = leaves.get(j, ND_IDX_MX)
            val ndMy: Int = leaves.get(j, ND_IDX_MY)
            val ndSx: Int = leaves.get(j, ND_IDX_SX)
            val ndSy: Int = leaves.get(j, ND_IDX_SY)
            val ndIndex: Int = leaves.get(j, ND_IDX_INDEX)
            val ndDepth: Int = leaves.get(j, ND_IDX_DEPTH)
            leafInsert(ndIndex, ndDepth, ndMx, ndMy, ndSx, ndSy, element)
        }
    }

    private fun leafInsert(node: Int, depth: Int, mx: Int, my: Int, sx: Int, sy: Int, element: Int) {
        // Insert the element node to the leaf.
        val ndFc: Int = nodes.get(node, NODE_IDX_FC)
        nodes.set(node, NODE_IDX_FC, enodes.insert())
        enodes.set(nodes.get(node, NODE_IDX_FC), ENODE_IDX_NEXT, ndFc)
        enodes.set(nodes.get(node, NODE_IDX_FC), ENODE_IDX_ELT, element)

        // If the leaf is full, split it.
        if (nodes.get(node, NODE_IDX_NUM) == maxElements && depth < maxDepth) {
            // Transfer elements from the leaf node to a list of elements.
            val elts: IntList = IntList(1)
            while (nodes.get(node, NODE_IDX_FC) != -1) {
                val index: Int = nodes.get(node, NODE_IDX_FC)
                val nextIndex: Int = enodes.get(index, ENODE_IDX_NEXT)
                val elt: Int = enodes.get(index, ENODE_IDX_ELT)

                // Pop off the element node from the leaf and remove it from the qt.
                nodes.set(node, NODE_IDX_FC, nextIndex)
                enodes.erase(index)

                // Insert element to the list.
                elts.set(elts.pushBack(), 0, elt)
            }

            // Start by allocating 4 child nodes.
            val fc: Int = nodes.insert()
            nodes.insert()
            nodes.insert()
            nodes.insert()
            nodes.set(node, NODE_IDX_FC, fc)

            // Initialize the new child nodes.
            for (j in 0..3) {
                nodes.set(fc + j, NODE_IDX_FC, -1)
                nodes.set(fc + j, NODE_IDX_NUM, 0)
            }

            // Transfer the elements in the former leaf node to its new children.
            nodes.set(node, NODE_IDX_NUM, -1)
            for (j in 0 until elts.size()) nodeInsert(node, depth, mx, my, sx, sy, elts.get(j, 0))
        } else {
            // Increment the leaf element count.
            nodes.set(node, NODE_IDX_NUM, nodes.get(node, NODE_IDX_NUM) + 1)
        }
    }


    // Stores all the element nodes in the quadtree.
    private val enodes: IntList = IntList(2)

    // Stores all the elements in the quadtree.
    private val elts: IntList = IntList(5)

    // Stores all the nodes in the quadtree. The first node in this
    // sequence is always the root.
    private val nodes: IntList = IntList(2)

    // ----------------------------------------------------------------------------------------
    // Data Members
    // ----------------------------------------------------------------------------------------
    // Temporary buffer used for queries.
    private var temp: BooleanArray = booleanArrayOf()

    // Stores the size of the temporary buffer.
    private var tempSize = 0

    // Stores the quadtree extents.
    private val rootMx: Int
    private val rootMy: Int
    private val rootSx: Int
    private val rootSy: Int

    // Creates a quadtree with the requested extents, maximum elements per leaf, and maximum tree depth.
    init {
        // Insert the root node to the qt.
        nodes.insert()
        nodes.set(0, NODE_IDX_FC, -1)
        nodes.set(0, NODE_IDX_NUM, 0)

        // Set the extents of the root node.
        rootMx = width / 2
        rootMy = height / 2
        rootSx = rootMx
        rootSy = rootMy
    }

    companion object {
        private fun floorInt(value: Float): Int {
            return value.toInt()
        }

        private fun intersect(
            l1: Int, t1: Int, r1: Int, b1: Int,
            l2: Int, t2: Int, r2: Int, b2: Int
        ): Boolean {
            return l2 <= r1 && r2 >= l1 && t2 <= b1 && b2 >= t1
        }

        private fun pushNode(nodes: IntList, ndIndex: Int, ndDepth: Int, ndMx: Int, ndMy: Int, ndSx: Int, ndSy: Int) {
            val backIdx: Int = nodes.pushBack()
            nodes.set(backIdx, ND_IDX_MX, ndMx)
            nodes.set(backIdx, ND_IDX_MY, ndMy)
            nodes.set(backIdx, ND_IDX_SX, ndSx)
            nodes.set(backIdx, ND_IDX_SY, ndSy)
            nodes.set(backIdx, ND_IDX_INDEX, ndIndex)
            nodes.set(backIdx, ND_IDX_DEPTH, ndDepth)
        }

        // ----------------------------------------------------------------------------------------
        // Element node fields:
        // ----------------------------------------------------------------------------------------
        // Points to the next element in the leaf node. A value of -1 
        // indicates the end of the list.
        const val ENODE_IDX_NEXT: Int = 0

        // Stores the element index.
        const val ENODE_IDX_ELT: Int = 1

        // ----------------------------------------------------------------------------------------
        // Element fields:
        // ----------------------------------------------------------------------------------------
        // Stores the rectangle encompassing the element.
        const val ELT_IDX_LFT: Int = 0
        const val ELT_IDX_TOP: Int = 1
        const val ELT_IDX_RGT: Int = 2
        const val ELT_IDX_BTM: Int = 3

        // Stores the ID of the element.
        const val ELT_IDX_ID: Int = 4

        // ----------------------------------------------------------------------------------------
        // Node fields:
        // ----------------------------------------------------------------------------------------
        // Points to the first child if this node is a branch or the first element
        // if this node is a leaf.
        const val NODE_IDX_FC: Int = 0

        // Stores the number of elements in the node or -1 if it is not a leaf.
        const val NODE_IDX_NUM: Int = 1

        // ----------------------------------------------------------------------------------------
        // Node data fields:
        // ----------------------------------------------------------------------------------------
        const val ND_NUM: Int = 6

        // Stores the extents of the node using a centered rectangle and half-size.
        const val ND_IDX_MX: Int = 0
        const val ND_IDX_MY: Int = 1
        const val ND_IDX_SX: Int = 2
        const val ND_IDX_SY: Int = 3

        // Stores the index of the node.
        const val ND_IDX_INDEX: Int = 4

        // Stores the depth of the node.
        const val ND_IDX_DEPTH: Int = 5

    }
}
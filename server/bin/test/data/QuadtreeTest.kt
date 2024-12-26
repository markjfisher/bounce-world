package data

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class QuadtreeTest {
    @Test
    fun `can create quad tree`() {
        val qt = Quadtree(100, 100, 100, 5)
        assertThat(qt).isNotNull
    }

    @Test
    fun `can add to and find single element in quadtree`() {
        val qt = Quadtree(100, 100, 1, 5)
        qt.insert(1, 10f, 10f, 15f, 15f)

        // non-intersecting
        val qEmpty = qt.query(50f, 50f, 60f, 60f)
        assertThat(qEmpty.size()).isEqualTo(0)

        // around whole node
        val qAround = qt.query(5f, 5f, 20f, 20f)
        assertThat(qAround.size()).isEqualTo(1)

        // intersecting
        val qIntersect = qt.query(12f, 12f, 20f, 20f)
        assertThat(qIntersect.size()).isEqualTo(1)

        // within the node
        val qInside = qt.query(12f, 12f, 14f, 14f)
        assertThat(qInside.size()).isEqualTo(1)
    }

    @Test
    fun `non intersecting query finds no elements`() {
        val qt = Quadtree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        val qEmpty = qt.query(50f, 50f, 60f, 60f)
        assertThat(qEmpty.size()).isEqualTo(0)
    }

    @Test
    fun `fully covered query finds all elements`() {
        val qt = Quadtree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        // fully around both
        val qAround = qt.query(5f, 5f, 30f, 30f)
        assertThat(qAround.size()).isEqualTo(2)

        // check we can use our helper function to return the element index values
        assertThat(qAround.toSet()).containsExactlyInAnyOrder(0, 1)

        // the element indexes are returned by get. These seem a bit pointless except for removal etc
        assertThat(qAround.get(0, 0)).isEqualTo(1)
        assertThat(qAround.get(1, 0)).isEqualTo(0)

        // show we can also extract the original IDs we gave to the elements
        val qAroundIds = qt.queryWithIds(5f, 5f, 30f, 30f)
        assertThat(qAroundIds).containsExactlyInAnyOrder(Pair(0, 69), Pair(1, 96))
    }

    @Test
    fun `multiple elements test`() {
        val qt = Quadtree(10000, 10000, 1, 5)
        qt.insert(69, 1000f, 1000f, 1500f, 1500f)
        qt.insert(96, 2000f, 1000f, 2500f, 1500f)
        qt.insert(111, 8000f, 8000f, 8500f, 8500f)
        qt.insert(222, 500f, 7000f, 1000f, 7500f)
        qt.insert(333, 4500f, 4500f, 7500f, 6000f)
        qt.insert(444, 6000f, 2000f, 8000f, 6500f)
        qt.insert(999, 100f, 100f, 9900f, 9900f)

        // partial query intersecting and within certain elements
        val qAround = qt.query(5500f, 4000f, 9000f, 8300f)

        // check we can use our helper function to return the element index values
        assertThat(qAround.toSet()).containsExactlyInAnyOrder(2, 4, 5, 6)

        // check we can also extract the original IDs we gave to the elements
        val qAroundIds = qt.queryWithIds(5500f, 4000f, 9000f, 8300f)
        assertThat(qAroundIds).containsExactlyInAnyOrder(
            Pair(2, 111),
            Pair(4, 333),
            Pair(5, 444),
            Pair(6, 999)
        )

        qt.traverse(visitor = object : IQtVisitor {
            override fun branch(qt: Quadtree, node: Int, depth: Int, mx: Int, my: Int, sx: Int, sy: Int) {
                println("Branch - Node: $node, Depth: $depth, Center: ($mx, $my), Half-Size: ($sx, $sy)")
            }

            override fun leaf(qt: Quadtree, node: Int, depth: Int, mx: Int, my: Int, sx: Int, sy: Int) {
                println("Leaf - Node: $node, Depth: $depth, Center: ($mx, $my), Half-Size: ($sx, $sy)")
            }
        })

    }

    @Test
    fun `intersecting but not whole covering query finds intersecting elements`() {
        val qt = Quadtree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        // intersecting both
        val qIntersectBoth = qt.queryWithIds(12f, 12f, 22f, 14f)
        assertThat(qIntersectBoth).containsExactlyInAnyOrder(Pair(0, 69), Pair(1, 96))
    }

    @Test
    fun `partial query finds covered element only`() {
        val qt = Quadtree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        // intersecting 1st
        val qIntersect1 = qt.queryWithIds(9f, 9f, 11f, 11f)
        assertThat(qIntersect1).containsExactly(Pair(0, 69))

        // intersecting 2nd
        val qIntersect2 = qt.queryWithIds(19f, 9f, 21f, 11f)
        assertThat(qIntersect2).containsExactly(Pair(1, 96))
    }

    @Test
    fun `can remove and re-add elements`() {
        val qt = Quadtree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        // removing '1' is the second element (zero based) so we removed id 96
        qt.remove(1)
        val qIntersectJustID1 = qt.queryWithIds(12f, 12f, 22f, 14f)
        assertThat(qIntersectJustID1).containsExactly(Pair(0, 69))

        // add it back in
        qt.insert(96, 20f, 10f, 25f, 15f)
        val qNew = qt.queryWithIds(12f, 12f, 22f, 14f)
        // we have the new 96 back in at index 1 as that was free
        assertThat(qNew).containsExactlyInAnyOrder(Pair(0, 69), Pair(1, 96))

        // remove first element
        qt.remove(0)
        val qNew2 = qt.queryWithIds(12f, 12f, 22f, 14f)
        // we have the new 96 back in at index 1 as that was free
        assertThat(qNew2).containsExactlyInAnyOrder(Pair(1, 96))

        // add it back in and see it's inserted at the front
        qt.insert(69, 10f, 10f, 15f, 15f)
        val qNew3 = qt.queryWithIds(12f, 12f, 22f, 14f)
        // we have the new 69 back in at index 0 as that was free
        assertThat(qNew3).containsExactlyInAnyOrder(Pair(0, 69), Pair(1, 96))
    }

    @Test
    fun `can traverse quadtree`() {
        val qt = Quadtree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        // Example of creating a visitor to get all the branches and leaves
        qt.traverse(visitor = object : IQtVisitor {
            override fun branch(qt: Quadtree, node: Int, depth: Int, mx: Int, my: Int, sx: Int, sy: Int) {
                println("Branch - Node: $node, Depth: $depth, Center: ($mx, $my), Half-Size: ($sx, $sy)")
            }

            override fun leaf(qt: Quadtree, node: Int, depth: Int, mx: Int, my: Int, sx: Int, sy: Int) {
                println("Leaf - Node: $node, Depth: $depth, Center: ($mx, $my), Half-Size: ($sx, $sy)")
            }
        })
        assertThat(qt).isNotNull
    }


}
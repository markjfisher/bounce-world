package data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class QuadTreeTest : StringSpec ({
    "can create quadtree" {
        QuadTree(100, 100, 100, 5) shouldNotBe null
    }

    "can add to and find single element in quadtree" {
        val qt = QuadTree(100, 100, 1, 5)
        qt.insert(1, 10f, 10f, 15f, 15f)

        // non-intersecting
        val qEmpty = qt.query(50f, 50f, 60f, 60f)
        qEmpty.size() shouldBe 0

        // around whole node
        val qAround = qt.query(5f, 5f, 20f, 20f)
        qAround.size() shouldBe 1

        // intersecting
        val qIntersect = qt.query(12f, 12f, 20f, 20f)
        qIntersect.size() shouldBe 1

        // within the node
        val qInside = qt.query(12f, 12f, 14f, 14f)
        qInside.size() shouldBe 1
    }

    "non intersecting query finds no elements" {
        val qt = QuadTree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        val qEmpty = qt.query(50f, 50f, 60f, 60f)
        qEmpty.size() shouldBe 0
    }

    "fully covered query finds all elements" {
        val qt = QuadTree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        // fully around both
        val qAround = qt.query(5f, 5f, 30f, 30f)
        qAround.size() shouldBe 2

        // check we can use our helper function to return the element index values
        qAround.toSet().shouldContainExactlyInAnyOrder(0, 1)

        // the element indexes are returned by get. These seem a bit pointless except for removal etc
        qAround.get(0, 0) shouldBe 1
        qAround.get(1, 0) shouldBe 0

        // show we can also extract the original IDs we gave to the elements
        val qAroundIds = qt.queryWithIds(5f, 5f, 30f, 30f)
        qAroundIds.shouldContainExactlyInAnyOrder(Pair(0, 69), Pair(1, 96))
    }

    "multiple elements test" {
        val qt = QuadTree(10000, 10000, 1, 5)
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
        qAround.toSet().shouldContainExactlyInAnyOrder(2, 4, 5, 6)

        // check we can also extract the original IDs we gave to the elements
        val qAroundIds = qt.queryWithIds(5500f, 4000f, 9000f, 8300f)
        qAroundIds.shouldContainExactlyInAnyOrder(
            Pair(2, 111),
            Pair(4, 333),
            Pair(5, 444),
            Pair(6, 999)
        )

//        qt.traverse(visitor = object : IQtVisitor {
//            override fun branch(qt: Quadtree, node: Int, depth: Int, mx: Int, my: Int, sx: Int, sy: Int) {
//                println("Branch - Node: $node, Depth: $depth, Center: ($mx, $my), Half-Size: ($sx, $sy)")
//            }
//
//            override fun leaf(qt: Quadtree, node: Int, depth: Int, mx: Int, my: Int, sx: Int, sy: Int) {
//                println("Leaf - Node: $node, Depth: $depth, Center: ($mx, $my), Half-Size: ($sx, $sy)")
//            }
//        })
    }

    "intersecting but not whole covering query finds intersecting elements" {
        val qt = QuadTree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        // intersecting both
        val qIntersectBoth = qt.queryWithIds(12f, 12f, 22f, 14f)
        qIntersectBoth.shouldContainExactlyInAnyOrder(Pair(0, 69), Pair(1, 96))
    }

    "partial query finds covered element only" {
        val qt = QuadTree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        // intersecting 1st
        val qIntersect1 = qt.queryWithIds(9f, 9f, 11f, 11f)
        qIntersect1 shouldContain Pair(0, 69)

        // intersecting 2nd
        val qIntersect2 = qt.queryWithIds(19f, 9f, 21f, 11f)
        qIntersect2 shouldContain Pair(1, 96)
    }

    "can remove and re-add elements" {
        val qt = QuadTree(100, 100, 1, 5)
        qt.insert(69, 10f, 10f, 15f, 15f)
        qt.insert(96, 20f, 10f, 25f, 15f)

        // removing '1' is the second element (zero based) so we removed id 96
        qt.remove(1)
        val qIntersectJustID1 = qt.queryWithIds(12f, 12f, 22f, 14f)
        qIntersectJustID1 shouldContain Pair(0, 69)

        // add it back in
        qt.insert(96, 20f, 10f, 25f, 15f)
        val qNew = qt.queryWithIds(12f, 12f, 22f, 14f)
        // we have the new 96 back in at index 1 as that was free
        qNew.shouldContainExactlyInAnyOrder(Pair(0, 69), Pair(1, 96))

        // remove first element
        qt.remove(0)
        val qNew2 = qt.queryWithIds(12f, 12f, 22f, 14f)
        // we have the new 96 back in at index 1 as that was free
        qNew2 shouldContain Pair(1, 96)

        // add it back in and see it's inserted at the front
        qt.insert(69, 10f, 10f, 15f, 15f)
        val qNew3 = qt.queryWithIds(12f, 12f, 22f, 14f)
        // we have the new 69 back in at index 0 as that was free
        qNew3.shouldContainExactlyInAnyOrder(Pair(0, 69), Pair(1, 96))
    }
})
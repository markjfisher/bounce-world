package collections

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class IntListTest : StringSpec({

    "size should return the correct number of elements" {
        val list = IntList(3) // Elements with 3 integer fields each
        list.pushBack()
        list.pushBack()
        list.size() shouldBe 2
    }

    "get and set should manipulate the correct values" {
        val list = IntList(2) // Elements with 2 integer fields each
        val index = list.pushBack()
        list.set(index, 0, 42) // Set first field of first element to 42
        list.set(index, 1, 24) // Set second field of first element to 24
        list.get(index, 0) shouldBe 42
        list.get(index, 1) shouldBe 24
    }

    "pushBack should add an element and return its index" {
        val list = IntList(1)
        val index1 = list.pushBack()
        val index2 = list.pushBack()
        index1 shouldBe 0
        index2 shouldBe 1
    }

    "popBack should remove the last element" {
        val list = IntList(1)
        list.pushBack()
        list.size() shouldBe 1
        list.popBack()
        list.size() shouldBe 0
    }

    "clear should remove all elements" {
        val list = IntList(2)
        list.pushBack()
        list.pushBack()
        list.clear()
        list.size() shouldBe 0
    }

    "insert should add an element correctly using a vacant position" {
        val list = IntList(1)
        val index1 = list.pushBack()
        list.erase(index1) // This should make the position vacant
        val index2 = list.insert() // This should reuse the vacant position
        index2 shouldBe index1
    }

    "erase should remove the specified element" {
        val list = IntList(1)
        val index = list.pushBack()
        list.size() shouldBe 1
        list.erase(index)
        list.insert() // This should reuse the position of the erased element
        list.size() shouldBe 1
    }

    "toSet should convert the list into a set correctly" {
        val list = IntList(1)
        list.pushBack()
        list.set(0, 0, 10)
        list.pushBack()
        list.set(1, 0, 20)
        val resultSet = list.toSet()
        resultSet shouldBe setOf(10, 20)
    }
})

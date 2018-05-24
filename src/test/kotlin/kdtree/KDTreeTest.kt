package kdtree

import models.Point
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertTrue

/**
 * TODO: add javadocs
 */
internal class KDTreeTest {
    var testData: MutableList<Point> = mutableListOf()

    internal class NeighborsTest {

        @Test
        fun testNeighbors() {
            val neighborList = KDTree.Neighbors(3)
            val first = KDTree.Neighbor(KDNode(location = Point(1, 1)), 1.0)
            val second = KDTree.Neighbor(KDNode(location = Point(3, 4)), 25.0)
            val third = KDTree.Neighbor(KDNode(location = Point(2, 5)), 29.0)
            val fourth = KDTree.Neighbor(KDNode(location = Point(5, 5)), 50.0)

            //add them out of order
            neighborList.add(second)
            neighborList.add(first)
            neighborList.add(fourth)
            neighborList.add(third)
            assertEquals(3, neighborList.size, "Wrong number of neighbors")
            val nit = neighborList.iterator()
            assertEquals(first, nit.next(), "Wrong order of neighbors")
            assertEquals(second, nit.next(), "Wrong order of neighbors")
            assertEquals(third, nit.next(), "Wrong order of neighbors")
        }

        private fun KDNode(location: Point): KDNode {
            return KDNode(null, location, 0, null, null)
        }
    }


    @BeforeEach
    fun setUp() {
        testData = mutableListOf(
                Point(0, 1),    //distance=1
                Point(0, -1),   //distance=1
                Point(1, 0),    //distance=1
                Point(-1, 0),   //distance=1
                Point(1, 1),    //distance=√2
                Point(-1, -1),  //distance=√2
                Point(3, 4),    //distance=5
                Point(-3, -4)   //distance=5
        )

        for (i in 3..200) {
            for (j in 3..200) {
                testData.add(Point(i, j))
            }
        }
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun testKDTree() {
        val tree = KDTree(testData.toMutableList())


        class nodeAndPlane(val node: KDNode, val plane: Int)
        val toCheck = Stack<nodeAndPlane>()

        toCheck.add(nodeAndPlane(tree.root, 0))
        var totalChecked = 0
        while (!toCheck.empty()) {
            val check = toCheck.pop()
            val parent = check.node
            val expectedPlane = check.plane

            val actualPlane = parent.plane
            assertEquals(expectedPlane, actualPlane, "Wrong plane for node $parent")

            parent.less?.let {
                assertTrue(it.location[actualPlane] <= parent.location[actualPlane], "'Less' node $it is not less than $parent")
                toCheck.add(nodeAndPlane(it, (expectedPlane + 1) % 2))
            }

            parent.greater?.let {
                assertTrue(it.location[actualPlane] > parent.location[actualPlane], "'Greater' node $it is not greater than $parent")
                toCheck.add(nodeAndPlane(it, (expectedPlane + 1) % 2))
            }


            totalChecked++
        }

        assertEquals(testData.size, totalChecked, "KDTree does not have the right number of elements")
    }

    @Test
    fun testNeighbors() {
        val tree = KDTree(testData.toMutableList())
        val neighbors = tree.nearestNeighbor(Point(0, 0), 4)

        assertEquals(4, neighbors.size, "Wrong number of neighbors found")

        //nearest neighbors to (0,0) are all 1 unit away
        assertTrue(
                neighbors.all { it.distance == 1.0 },
                "Nearest neighbors not found"
        )

    }
}
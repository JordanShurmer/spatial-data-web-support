package kdtree

import models.Locatable
import models.Point
import models.ViewerData
import java.util.*
import kotlin.math.absoluteValue

/**
 * TODO: add javadocs
 */

class KDNode(
        val data: Any?,
        val location: Point,
        val plane: Int,
        var less: KDNode?,
        var greater: KDNode?
) : Comparable<KDNode> {

    constructor() : this(ViewerData(), Point(0.0, 0.0), 0, null, null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KDNode) return super.equals(other)

        return location == other.location
    }

    override fun hashCode(): Int {
        return location.hashCode()
    }



    override fun compareTo(other: KDNode): Int {
        return location.compareTo(other.location)
    }

    override fun toString(): String {
        return "KDNode(location=$location, plane=$plane, hasLess=${less != null}, hasGreater=${greater != null})"
    }

}

class KDTree(data: MutableList<Locatable>) {
    val root: KDNode

    init {
        // Recurse to build the whole tree
        fun buildTree(curSet: MutableList<Locatable>, d: Int = 0): KDNode? {
            if (curSet.size == 0) return null

            //sort by the current dimension
            curSet.sortBy { it.location[d] }

            //split the current set in half
            var mid = curSet.size / 2
            var median = curSet[mid]

            //put the equal nodes to the left
            while (mid + 1 < curSet.size && curSet[mid + 1].location[d] == median.location[d]) median = curSet[++mid]

            //cycle through the dimensions
            val newD = (d + 1) % 2

            //return the root, and recursively build each side of the tree
            return KDNode(
                    data = median,
                    location = median.location,
                    plane = d,
                    less = buildTree(curSet.subList(0, mid), newD),
                    greater = buildTree(curSet.subList(mid + 1, curSet.size), newD)
            )
        }

        this.root = when {
            data.size == 0 -> KDNode()
            else -> buildTree(data)!! //NPE avoided by size check above
        }
    }


    class Neighbor(val neighbor: KDNode, val distance: Double)

    class Neighbors(
            private val count: Int,
            private val delegate: SortedSet<Neighbor> = TreeSet<Neighbor>(compareBy({ it.distance}, { it.neighbor }))
    ) : SortedSet<Neighbor> by delegate {

        fun furthestDistance(): Double {
            if (delegate.isEmpty()) {
                return Double.MAX_VALUE
            }
            return delegate.last().distance
        }

        override fun add(element: Neighbor?): Boolean {
            if (delegate.add(element)) {
                if (delegate.size > count)
                    delegate.remove(delegate.last())
                return true
            }
            return false
        }
    }

    fun nearestNeighbor(fromHere: Point, n: Int): Neighbors {
        val neighbors = Neighbors(n)

        fun crawlTree(start: KDNode) {

            //*
            //Find the leaf node this would pair with
            //*
            val pathToLeaf = Stack<KDNode>()
            var next: KDNode? = start
            while (next != null) {
                pathToLeaf.push(next)
                val plane = next.plane
                next = when {
                    fromHere.location[plane] <= next.location[plane] -> next.less
                    else -> next.greater
                }
            }

            //*
            //Then work back up the tree, checking the other side of each parent for potentially closer nodes
            //*
            var previous: KDNode? = null
            while (!pathToLeaf.empty()) {
                val check = pathToLeaf.pop()

                //check the distance to this node
                var distance = check.location.distanceSquared(fromHere.location)
                if (distance <= neighbors.furthestDistance()) {
                    neighbors.add(Neighbor(check, distance))
                }

                //then see if any on the other side of this contains anything closer
                var subTree: KDNode? = null
                if (previous == check.less) {
                    subTree = check.greater
                } else if (previous == check.greater) {
                    subTree = check.less
                }
                previous = check
                if (subTree != null) {
                    //just the distance on the splitting plane
                    val split = subTree.plane
                    distance = (fromHere.location[split] - subTree.location[split]).absoluteValue
                    if (distance <= neighbors.furthestDistance()) {
                        //recurse this subtree looking for neighbors
                        crawlTree(subTree)
                    }
                }

            }
        }

        crawlTree(root)

//        return neighbors.map { it.neighbor }
        return neighbors
    }

}


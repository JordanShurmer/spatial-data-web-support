package models

import kdtree.KDNode
import kotlin.math.pow

/**
 * TODO: add javadocs
 */
interface Locatable {
    val location: Point
}

class Point(val lat: Double, val lng: Double) : Locatable, Comparable<Point> {

    constructor(latI: Int, lngI: Int) : this(latI.toDouble(), lngI.toDouble())
    constructor(lat: Double, lngI: Int) : this(lat, lngI.toDouble())
    constructor(latI: Int, lng: Double) : this(latI.toDouble(), lng)

    override val location = this

    operator fun get(i: Int): Double {
        return when {
            i % 2 == 0 -> lat
            else -> lng
        }
    }

    fun distanceSquared(point: Point): Double {
        return (lat - point.lat).pow(2) + (lng - point.lng).pow(2)
    }

    override fun toString(): String {
        return "Point(lat=$lat, lng=$lng)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point

        if (lat != other.lat) return false
        if (lng != other.lng) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lat.hashCode()
        result = 31 * result + lng.hashCode()
        return result
    }

    override fun compareTo(other: Point): Int {
        return compareBy<Point>({ it.lat }, { it.lng }).compare(this, other)
    }
}

class Rectange(val min: Point, val max: Point)

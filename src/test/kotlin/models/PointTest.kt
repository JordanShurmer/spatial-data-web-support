package models

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * TODO: add javadocs
 */
internal class PointTest {

    @Test
    fun distanceSquared() {
        assertEquals(
                0.0,
                Point(0.0, 0.0).distanceSquared(Point(0.0, 0.0))
        )

        assertEquals(
                25.0,
                Point(0.0, 0.0).distanceSquared(Point(3.0, 4.0))
        )

        assertEquals(
                25.0,
                Point(3.0, 4.0).distanceSquared(Point(0.0, 0.0))
        )
    }
}
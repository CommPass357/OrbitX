package com.commpass357.orbitx.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Vector2Test {
    @Test
    fun vectorMathTracksLengthAndNormalization() {
        val vector = Vector2(3.0, 4.0)

        assertEquals(5.0, vector.length, 1.0e-9)
        assertEquals(Vector2(0.6, 0.8), vector.normalized())
        assertEquals(Vector2(-4.0, 3.0), vector.perpendicularLeft())
    }

    @Test
    fun zeroVectorNormalizesToZero() {
        assertEquals(Vector2.ZERO, Vector2.ZERO.normalized())
    }
}

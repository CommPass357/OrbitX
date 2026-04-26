package com.commpass357.orbitx.sim

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class Vector2(val x: Double, val y: Double) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scale: Double) = Vector2(x * scale, y * scale)
    operator fun div(scale: Double) = Vector2(x / scale, y / scale)
    operator fun unaryMinus() = Vector2(-x, -y)

    val length: Double
        get() = hypot(x, y)

    val lengthSquared: Double
        get() = x * x + y * y

    fun normalized(): Vector2 {
        val len = length
        return if (len <= 1.0e-12) ZERO else this / len
    }

    fun distanceTo(other: Vector2): Double = (this - other).length

    fun perpendicularLeft(): Vector2 = Vector2(-y, x)

    fun rotate(radians: Double): Vector2 {
        val c = cos(radians)
        val s = sin(radians)
        return Vector2(x * c - y * s, x * s + y * c)
    }

    companion object {
        val ZERO = Vector2(0.0, 0.0)

        fun fromPolar(radius: Double, angleRadians: Double): Vector2 {
            return Vector2(radius * cos(angleRadians), radius * sin(angleRadians))
        }
    }
}

operator fun Double.times(vector: Vector2): Vector2 = vector * this

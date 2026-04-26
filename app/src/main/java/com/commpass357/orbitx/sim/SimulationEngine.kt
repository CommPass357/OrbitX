package com.commpass357.orbitx.sim

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class SimulationEngine(
    initialBodies: List<BodyState>,
    private val settings: SimulationSettings = SimulationSettings(),
    initialTimeDays: Double = 0.0
) {
    private val random = Random(17357)
    private var bodies = initialBodies.map { it.copy(trail = it.trail.takeLast(settings.trailLimit)) }
    private var events = emptyList<SimulationEvent>()
    private var nextId = (initialBodies.maxOfOrNull { it.id } ?: 0L) + 1L

    var timeDays: Double = initialTimeDays
        private set

    fun snapshot(): SimulationSnapshot = SimulationSnapshot(
        timeDays = timeDays,
        bodies = bodies,
        events = events
    )

    fun reset(initialBodies: List<BodyState>) {
        bodies = initialBodies.map { it.copy(trail = emptyList()) }
        events = emptyList()
        nextId = (initialBodies.maxOfOrNull { it.id } ?: 0L) + 1L
        timeDays = 0.0
    }

    fun body(id: Long): BodyState? = bodies.firstOrNull { it.id == id }

    fun nearestBody(position: Vector2, maxDistance: Double): BodyState? {
        return bodies
            .map { it to it.position.distanceTo(position) }
            .filter { it.second <= maxDistance }
            .minByOrNull { it.second }
            ?.first
    }

    fun stepSeconds(seconds: Double, timeScaleDaysPerSecond: Double) {
        val days = (seconds * timeScaleDaysPerSecond).coerceIn(0.0, 96.0)
        stepDays(days)
    }

    fun stepDays(days: Double) {
        if (days <= 0.0 || bodies.isEmpty()) return
        var remaining = days
        var guard = 0
        while (remaining > 1.0e-9 && guard < 2_000) {
            val dt = min(settings.fixedStepDays, remaining)
            integrate(dt)
            handleCollisions()
            handleEscapes()
            timeDays += dt
            remaining -= dt
            guard++
        }
    }

    fun spawnPreset(preset: SpawnPreset, position: Vector2, velocity: Vector2 = Vector2.ZERO): Long? {
        if (bodies.size >= settings.maxBodies) {
            addEvent("Population cap", "Remove a few objects before spawning more.")
            return null
        }
        val newId = nextId++
        val body = BodyState(
            id = newId,
            name = preset.name,
            type = preset.type,
            mass = preset.mass,
            drawRadius = preset.drawRadius,
            collisionRadius = preset.collisionRadius,
            colorArgb = preset.colorArgb,
            position = position,
            velocity = velocity,
            locked = false
        )
        bodies = bodies + body
        addEvent("Spawned ${preset.name}", "Mass ${preset.mass.formatMass()} at ${position.formatPosition()}.")
        return newId
    }

    fun updateBody(id: Long, transform: (BodyState) -> BodyState) {
        bodies = bodies.map { body ->
            if (body.id == id) {
                transform(body).copy(trail = body.trail.takeLast(settings.trailLimit))
            } else {
                body
            }
        }
    }

    fun moveBody(id: Long, position: Vector2) {
        updateBody(id) { it.copy(position = position, trail = it.trail.takeLast(24)) }
    }

    fun setVelocity(id: Long, velocity: Vector2) {
        updateBody(id) { it.copy(velocity = velocity) }
    }

    fun applyImpulse(id: Long, deltaVelocity: Vector2) {
        updateBody(id) { it.copy(velocity = it.velocity + deltaVelocity) }
    }

    fun toggleLocked(id: Long) {
        updateBody(id) { it.copy(locked = !it.locked) }
    }

    fun deleteBody(id: Long) {
        val removed = body(id) ?: return
        bodies = bodies.filterNot { it.id == id }
        addEvent("Removed ${removed.name}", "The system rebalances without it.")
    }

    fun circularizeAroundPrimary(id: Long) {
        val target = body(id) ?: return
        val primary = bodies
            .filterNot { it.id == id }
            .maxByOrNull { it.mass }
            ?: return
        val offset = target.position - primary.position
        val radius = offset.length
        if (radius <= settings.softening) return
        val speed = kotlin.math.sqrt(settings.gravitationalConstant * primary.mass / radius)
        val direction = offset.normalized().perpendicularLeft()
        val newVelocity = primary.velocity + direction * speed
        setVelocity(id, newVelocity)
        addEvent("Circularized ${target.name}", "Velocity matched around ${primary.name}.")
    }

    fun updateMass(id: Long, factor: Double) {
        updateBody(id) {
            it.copy(
                mass = (it.mass * factor).coerceIn(1.0e-14, 2.5),
                drawRadius = (it.drawRadius * kotlin.math.sqrt(factor)).coerceIn(0.002, 0.45),
                collisionRadius = (it.collisionRadius * kotlin.math.sqrt(factor)).coerceIn(0.0003, 0.15)
            )
        }
    }

    fun predictTrajectories(
        selectedId: Long?,
        horizonDays: Double = 620.0,
        sampleEveryDays: Double = 7.0
    ): Map<Long, List<Vector2>> {
        val simulated = SimulationEngine(
            initialBodies = bodies.map { it.copy(trail = emptyList()) },
            settings = settings.copy(trailLimit = 0),
            initialTimeDays = timeDays
        )
        val trackedIds = bodies
            .filter { it.id == selectedId || it.type != BodyType.Asteroid && it.type != BodyType.Fragment }
            .map { it.id }
            .toSet()
        val paths = trackedIds.associateWith { mutableListOf<Vector2>() }.toMutableMap()
        var elapsed = 0.0
        while (elapsed < horizonDays) {
            simulated.stepDays(sampleEveryDays)
            simulated.snapshot().bodies.forEach { body ->
                paths[body.id]?.add(body.position)
            }
            elapsed += sampleEveryDays
        }
        return paths.mapValues { it.value.toList() }
    }

    private fun integrate(dt: Double) {
        val accelerations = MutableList(bodies.size) { Vector2.ZERO }
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val a = bodies[i]
                val b = bodies[j]
                val delta = b.position - a.position
                val distanceSquared = delta.lengthSquared + settings.softening * settings.softening
                val direction = delta.normalized()
                val accelA = direction * (settings.gravitationalConstant * b.mass / distanceSquared)
                val accelB = -direction * (settings.gravitationalConstant * a.mass / distanceSquared)
                if (!a.locked) accelerations[i] = accelerations[i] + accelA
                if (!b.locked) accelerations[j] = accelerations[j] + accelB
            }
        }

        bodies = bodies.mapIndexed { index, body ->
            if (body.locked) {
                body.copy(trail = appendTrail(body, body.position))
            } else {
                val velocity = body.velocity + accelerations[index] * dt
                val position = body.position + velocity * dt
                body.copy(
                    position = position,
                    velocity = velocity,
                    trail = appendTrail(body, position)
                )
            }
        }
    }

    private fun appendTrail(body: BodyState, position: Vector2): List<Vector2> {
        if (settings.trailLimit <= 0) return emptyList()
        if (body.trail.lastOrNull()?.distanceTo(position)?.let { it < 0.015 } == true) {
            return body.trail
        }
        return (body.trail + position).takeLast(settings.trailLimit)
    }

    private fun handleCollisions() {
        val consumed = mutableSetOf<Long>()
        val additions = mutableListOf<BodyState>()
        val survivors = mutableListOf<BodyState>()

        for (i in bodies.indices) {
            val a = bodies[i]
            if (a.id in consumed) continue
            var merged: BodyState? = null
            for (j in i + 1 until bodies.size) {
                val b = bodies[j]
                if (b.id in consumed) continue
                val collisionDistance = max(0.0006, a.collisionRadius + b.collisionRadius)
                if (a.position.distanceTo(b.position) <= collisionDistance) {
                    consumed += a.id
                    consumed += b.id
                    val relativeSpeed = (a.velocity - b.velocity).length
                    if (a.isSmallDebris && b.isSmallDebris && relativeSpeed > 0.012) {
                        additions += fragmentCollision(a, b)
                    } else {
                        merged = mergeBodies(a, b)
                        additions += merged
                    }
                    break
                }
            }
            if (a.id !in consumed) survivors += a
        }

        if (consumed.isNotEmpty()) {
            bodies = (survivors + additions).take(settings.maxBodies)
        }
    }

    private fun mergeBodies(a: BodyState, b: BodyState): BodyState {
        val dominant = if (a.mass >= b.mass) a else b
        val totalMass = a.mass + b.mass
        val position = (a.position * a.mass + b.position * b.mass) / totalMass
        val velocity = (a.velocity * a.mass + b.velocity * b.mass) / totalMass
        val merged = dominant.copy(
            id = nextId++,
            name = "${dominant.name}+",
            mass = totalMass,
            drawRadius = kotlin.math.sqrt(a.drawRadius * a.drawRadius + b.drawRadius * b.drawRadius).coerceAtMost(0.5),
            collisionRadius = kotlin.math.sqrt(
                a.collisionRadius * a.collisionRadius + b.collisionRadius * b.collisionRadius
            ).coerceAtMost(0.18),
            position = position,
            velocity = velocity,
            locked = a.locked || b.locked,
            trail = (a.trail + b.trail).takeLast(settings.trailLimit)
        )
        addEvent("Impact: ${a.name} + ${b.name}", "${merged.name} inherits their momentum.")
        return merged
    }

    private fun fragmentCollision(a: BodyState, b: BodyState): List<BodyState> {
        val totalMass = (a.mass + b.mass) * 0.72
        val center = (a.position * a.mass + b.position * b.mass) / (a.mass + b.mass)
        val baseVelocity = (a.velocity * a.mass + b.velocity * b.mass) / (a.mass + b.mass)
        val fragments = List(3) { index ->
            val angle = random.nextDouble(0.0, PI * 2.0)
            val kick = Vector2.fromPolar(0.002 + index * 0.0015, angle)
            BodyState(
                id = nextId++,
                name = "Fragment ${nextId - 1}",
                type = BodyType.Fragment,
                mass = totalMass / 3.0,
                drawRadius = 0.008,
                collisionRadius = 0.0008,
                colorArgb = 0xFFB7C2D9,
                position = center + Vector2.fromPolar(0.004 * (index + 1), angle),
                velocity = baseVelocity + kick,
                trail = emptyList()
            )
        }
        addEvent("Shattered debris", "${a.name} and ${b.name} broke into fragments.")
        return fragments
    }

    private fun handleEscapes() {
        val escaped = bodies.filter { it.position.length > settings.escapeRadius }
        if (escaped.isEmpty()) return
        bodies = bodies.filter { it.position.length <= settings.escapeRadius }
        escaped.take(3).forEach { body ->
            addEvent("${body.name} escaped", "It crossed ${settings.escapeRadius.toInt()} AU from the Sun.")
        }
    }

    private fun addEvent(title: String, detail: String) {
        events = (listOf(SimulationEvent(timeDays, title, detail)) + events).take(8)
    }
}

private fun Double.formatMass(): String = when {
    this >= 1.0 -> "%.2f Suns".format(this)
    this >= 1.0e-3 -> "%.2f Jupiter".format(this / 9.545e-4)
    this >= 1.0e-6 -> "%.2f Earth".format(this / 3.003e-6)
    else -> "%.2e".format(this)
}

private fun Vector2.formatPosition(): String = "(%.2f, %.2f) AU".format(x, y)

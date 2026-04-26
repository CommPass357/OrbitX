package com.commpass357.orbitx.sim

import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.random.Random

object SolarSystemScenario {
    private const val SunMass = 1.0
    private const val G = 2.9591220828559093e-4

    val spawnPresets = listOf(
        SpawnPreset("Rocky planet", BodyType.Planet, 3.0e-6, 0.042, 0.0028, 0xFF6BCB77, 0.006),
        SpawnPreset("Gas giant", BodyType.Planet, 7.5e-4, 0.1, 0.009, 0xFFE9B872, 0.0035),
        SpawnPreset("Moonlet", BodyType.Moon, 4.0e-8, 0.023, 0.0011, 0xFFD7DEE8, 0.004),
        SpawnPreset("Dwarf planet", BodyType.DwarfPlanet, 7.0e-9, 0.03, 0.0012, 0xFFB89C7D, 0.004),
        SpawnPreset("Asteroid", BodyType.Asteroid, 1.0e-11, 0.014, 0.0006, 0xFF9EA7B3, 0.005),
        SpawnPreset("Comet", BodyType.Comet, 5.0e-12, 0.018, 0.0007, 0xFFB8F7FF, 0.008),
        SpawnPreset("Probe", BodyType.Probe, 1.0e-14, 0.018, 0.0004, 0xFFFFF2A8, 0.012),
        SpawnPreset("Custom body", BodyType.Custom, 1.0e-7, 0.03, 0.0016, 0xFFFF7AC8, 0.005)
    )

    fun create(): List<BodyState> {
        var id = 1L
        val bodies = mutableListOf<BodyState>()

        fun add(body: BodyState) {
            bodies += body.copy(id = id++)
        }

        val sun = BodyState(
            id = 0,
            name = "Sun",
            type = BodyType.Star,
            mass = SunMass,
            drawRadius = 0.24,
            collisionRadius = 0.035,
            colorArgb = 0xFFFFD36A,
            position = Vector2.ZERO,
            velocity = Vector2.ZERO,
            locked = true
        )
        add(sun)

        fun planet(
            name: String,
            mass: Double,
            orbitAu: Double,
            angle: Double,
            drawRadius: Double,
            collisionRadius: Double,
            color: Long
        ): BodyState {
            val position = Vector2.fromPolar(orbitAu, angle)
            val speed = sqrt(G * SunMass / orbitAu)
            return BodyState(
                id = 0,
                name = name,
                type = BodyType.Planet,
                mass = mass,
                drawRadius = drawRadius,
                collisionRadius = collisionRadius,
                colorArgb = color,
                position = position,
                velocity = position.normalized().perpendicularLeft() * speed
            )
        }

        val mercury = planet("Mercury", 1.66e-7, 0.39, 0.3, 0.026, 0.0013, 0xFFB7A99A)
        val venus = planet("Venus", 2.45e-6, 0.72, 1.2, 0.038, 0.0027, 0xFFE7C37D)
        val earth = planet("Earth", 3.003e-6, 1.0, 2.15, 0.043, 0.0029, 0xFF4DA3FF)
        val mars = planet("Mars", 3.23e-7, 1.52, 3.0, 0.034, 0.0018, 0xFFFF7A59)
        val jupiter = planet("Jupiter", 9.545e-4, 5.2, 0.65, 0.115, 0.011, 0xFFE0B36D)
        val saturn = planet("Saturn", 2.858e-4, 9.54, 1.9, 0.105, 0.0095, 0xFFEBD99B)
        val uranus = planet("Uranus", 4.366e-5, 19.2, 4.4, 0.075, 0.0065, 0xFF84E3E0)
        val neptune = planet("Neptune", 5.151e-5, 30.1, 5.15, 0.075, 0.0063, 0xFF5277FF)
        listOf(mercury, venus, earth, mars, jupiter, saturn, uranus, neptune).forEach(::add)

        fun moon(
            name: String,
            parent: BodyState,
            mass: Double,
            orbitAu: Double,
            angle: Double,
            drawRadius: Double,
            collisionRadius: Double,
            color: Long
        ) {
            val offset = Vector2.fromPolar(orbitAu, angle)
            val speed = sqrt(G * parent.mass / orbitAu)
            add(
                BodyState(
                    id = 0,
                    name = name,
                    type = BodyType.Moon,
                    mass = mass,
                    drawRadius = drawRadius,
                    collisionRadius = collisionRadius,
                    colorArgb = color,
                    position = parent.position + offset,
                    velocity = parent.velocity + offset.normalized().perpendicularLeft() * speed
                )
            )
        }

        moon("Moon", earth, 3.69e-8, 0.0045, 0.2, 0.023, 0.0011, 0xFFD9DEE8)
        moon("Phobos", mars, 5.4e-14, 0.0022, 1.6, 0.014, 0.0005, 0xFFB7A18D)
        moon("Deimos", mars, 7.5e-15, 0.0039, 3.8, 0.012, 0.0004, 0xFFC9B9A5)
        moon("Io", jupiter, 4.49e-8, 0.0042, 0.7, 0.023, 0.0011, 0xFFFFD66B)
        moon("Europa", jupiter, 2.41e-8, 0.0067, 2.2, 0.022, 0.001, 0xFFEFE5CD)
        moon("Ganymede", jupiter, 7.43e-8, 0.0107, 3.1, 0.028, 0.0014, 0xFFB6A38A)
        moon("Callisto", jupiter, 5.43e-8, 0.0188, 4.4, 0.026, 0.0013, 0xFF7E746D)
        moon("Titan", saturn, 6.76e-8, 0.0122, 0.9, 0.029, 0.0015, 0xFFE5A849)
        moon("Enceladus", saturn, 5.45e-11, 0.0040, 3.5, 0.018, 0.0008, 0xFFEFF9FF)
        moon("Triton", neptune, 1.08e-8, 0.0060, 2.7, 0.023, 0.0011, 0xFFC8D7E8)

        add(planet("Ceres", 4.73e-10, 2.77, 5.2, 0.028, 0.001, 0xFFAFA8A0).copy(type = BodyType.DwarfPlanet))
        add(planet("Pluto", 6.55e-9, 39.5, 0.15, 0.032, 0.0013, 0xFFD7B899).copy(type = BodyType.DwarfPlanet))

        addAsteroidBelt(bodies, id)
        id = (bodies.maxOfOrNull { it.id } ?: id) + 1L

        fun comet(name: String, distance: Double, angle: Double, color: Long) {
            val position = Vector2.fromPolar(distance, angle)
            val circularSpeed = sqrt(G * SunMass / distance)
            add(
                BodyState(
                    id = 0,
                    name = name,
                    type = BodyType.Comet,
                    mass = 4.0e-12,
                    drawRadius = 0.018,
                    collisionRadius = 0.0006,
                    colorArgb = color,
                    position = position,
                    velocity = position.normalized().perpendicularLeft() * (circularSpeed * 0.32)
                )
            )
        }
        comet("Halley spark", 18.0, 2.65, 0xFFC7F9FF)
        comet("Kite", 24.0, 4.8, 0xFFDDFEFF)
        comet("Longtail", 32.0, 1.15, 0xFFB8F7FF)

        add(
            BodyState(
                id = 0,
                name = "Voyager seed",
                type = BodyType.Probe,
                mass = 1.0e-14,
                drawRadius = 0.018,
                collisionRadius = 0.00035,
                colorArgb = 0xFFFFF2A8,
                position = earth.position + Vector2(0.035, 0.0),
                velocity = earth.velocity + Vector2(0.002, 0.014)
            )
        )

        return bodies
    }

    private fun addAsteroidBelt(bodies: MutableList<BodyState>, firstId: Long) {
        val random = Random(357)
        var id = firstId
        repeat(52) { index ->
            val orbitAu = random.nextDouble(2.15, 3.45)
            val angle = random.nextDouble(0.0, PI * 2.0)
            val position = Vector2.fromPolar(orbitAu, angle)
            val speed = sqrt(G * SunMass / orbitAu) * random.nextDouble(0.965, 1.035)
            bodies += BodyState(
                id = id++,
                name = "Asteroid ${index + 1}",
                type = BodyType.Asteroid,
                mass = random.nextDouble(2.0e-13, 3.0e-11),
                drawRadius = random.nextDouble(0.009, 0.016),
                collisionRadius = random.nextDouble(0.00035, 0.0008),
                colorArgb = listOf(0xFF9EA7B3, 0xFFB7A18D, 0xFF7F8794).random(random),
                position = position,
                velocity = position.normalized().perpendicularLeft() * speed
            )
        }
    }
}

package com.commpass357.orbitx.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SimulationEngineTest {
    @Test
    fun twoBodyOrbitStaysBoundOverSeveralYears() {
        val g = SimulationSettings().gravitationalConstant
        val earth = BodyState(
            id = 2,
            name = "Test Earth",
            type = BodyType.Planet,
            mass = 3.003e-6,
            drawRadius = 0.04,
            collisionRadius = 0.001,
            colorArgb = 0xFF4DA3FF,
            position = Vector2(1.0, 0.0),
            velocity = Vector2(0.0, sqrt(g))
        )
        val engine = SimulationEngine(listOf(testSun(), earth))

        repeat(12) { engine.stepDays(30.0) }

        val finalEarth = engine.body(2)!!
        assertTrue(finalEarth.position.length in 0.82..1.2)
    }

    @Test
    fun collisionConservesMomentumWhenBodiesMerge() {
        val a = testBody(1, "A", mass = 2.0e-6, position = Vector2(-0.0002, 0.0), velocity = Vector2(0.01, 0.0))
        val b = testBody(2, "B", mass = 1.0e-6, position = Vector2(0.0002, 0.0), velocity = Vector2(-0.02, 0.0))
        val engine = SimulationEngine(listOf(a, b), SimulationSettings(softening = 0.0001))

        engine.stepDays(0.05)

        val merged = engine.snapshot().bodies.single()
        assertEquals(3.0e-6, merged.mass, 1.0e-12)
        assertEquals(0.0, merged.velocity.x, 1.0e-4)
    }

    @Test
    fun escapedBodiesAreRemovedAndLogged() {
        val engine = SimulationEngine(
            listOf(
                testSun(),
                testBody(2, "Runner", position = Vector2(65.0, 0.0), velocity = Vector2(1.0, 0.0))
            )
        )

        engine.stepDays(0.1)

        assertEquals(null, engine.body(2))
        assertTrue(engine.snapshot().events.first().title.contains("escaped"))
    }

    @Test
    fun scenarioAsteroidsAreDeterministic() {
        val first = SolarSystemScenario.create().filter { it.type == BodyType.Asteroid }.take(5)
        val second = SolarSystemScenario.create().filter { it.type == BodyType.Asteroid }.take(5)

        assertEquals(first.map { it.position }, second.map { it.position })
        assertEquals(52, SolarSystemScenario.create().count { it.type == BodyType.Asteroid })
    }

    @Test
    fun predictionDoesNotMutateLiveState() {
        val engine = SimulationEngine(SolarSystemScenario.create())
        val before = engine.snapshot()

        val paths = engine.predictTrajectories(selectedId = before.bodies.first().id, horizonDays = 30.0, sampleEveryDays = 5.0)
        val after = engine.snapshot()

        assertTrue(paths.isNotEmpty())
        assertEquals(before.timeDays, after.timeDays, 0.0)
        assertEquals(before.bodies.map { it.position }, after.bodies.map { it.position })
    }

    @Test
    fun spawnPresetAddsBody() {
        val engine = SimulationEngine(listOf(testSun()))
        val preset = SolarSystemScenario.spawnPresets.first()

        val id = engine.spawnPreset(preset, Vector2(1.5, 0.0), Vector2(0.0, 0.01))

        assertNotEquals(null, id)
        assertEquals(2, engine.snapshot().bodies.size)
    }

    private fun testSun() = BodyState(
        id = 1,
        name = "Sun",
        type = BodyType.Star,
        mass = 1.0,
        drawRadius = 0.2,
        collisionRadius = 0.02,
        colorArgb = 0xFFFFD36A,
        position = Vector2.ZERO,
        velocity = Vector2.ZERO,
        locked = true
    )

    private fun testBody(
        id: Long,
        name: String,
        mass: Double = 1.0e-6,
        position: Vector2,
        velocity: Vector2
    ) = BodyState(
        id = id,
        name = name,
        type = BodyType.Planet,
        mass = mass,
        drawRadius = 0.02,
        collisionRadius = 0.01,
        colorArgb = 0xFFFFFFFF,
        position = position,
        velocity = velocity
    )
}

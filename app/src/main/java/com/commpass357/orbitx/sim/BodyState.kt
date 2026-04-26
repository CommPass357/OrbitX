package com.commpass357.orbitx.sim

data class BodyState(
    val id: Long,
    val name: String,
    val type: BodyType,
    val mass: Double,
    val drawRadius: Double,
    val collisionRadius: Double,
    val colorArgb: Long,
    val position: Vector2,
    val velocity: Vector2,
    val locked: Boolean = false,
    val trail: List<Vector2> = emptyList()
) {
    val isSmallDebris: Boolean
        get() = type == BodyType.Asteroid || type == BodyType.Comet || type == BodyType.Fragment || type == BodyType.Probe
}

data class SimulationSettings(
    val gravitationalConstant: Double = 2.9591220828559093e-4,
    val softening: Double = 0.0015,
    val fixedStepDays: Double = 0.75,
    val trailLimit: Int = 96,
    val escapeRadius: Double = 64.0,
    val maxBodies: Int = 140
)

data class SimulationEvent(
    val timeDays: Double,
    val title: String,
    val detail: String
)

data class SimulationSnapshot(
    val timeDays: Double,
    val bodies: List<BodyState>,
    val events: List<SimulationEvent>
)

data class SpawnPreset(
    val name: String,
    val type: BodyType,
    val mass: Double,
    val drawRadius: Double,
    val collisionRadius: Double,
    val colorArgb: Long,
    val defaultSpeed: Double
)

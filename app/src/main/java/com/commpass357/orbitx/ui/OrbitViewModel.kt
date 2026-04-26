package com.commpass357.orbitx.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.commpass357.orbitx.sim.BodyState
import com.commpass357.orbitx.sim.BodyType
import com.commpass357.orbitx.sim.SimulationEngine
import com.commpass357.orbitx.sim.SimulationSnapshot
import com.commpass357.orbitx.sim.SolarSystemScenario
import com.commpass357.orbitx.sim.SpawnPreset
import com.commpass357.orbitx.sim.Vector2
import kotlin.math.sqrt

enum class GameTool(val label: String) {
    Navigate("Navigate"),
    Move("Move"),
    Velocity("Velocity")
}

data class OrbitUiState(
    val snapshot: SimulationSnapshot,
    val selectedId: Long? = null,
    val isPlaying: Boolean = true,
    val activeTool: GameTool = GameTool.Navigate,
    val timeScale: Double = 36.0,
    val showTrails: Boolean = true,
    val showPrediction: Boolean = true,
    val paletteOpen: Boolean = false,
    val predictionPaths: Map<Long, List<Vector2>> = emptyMap()
) {
    val selectedBody: BodyState?
        get() = selectedId?.let { id -> snapshot.bodies.firstOrNull { it.id == id } }
}

class OrbitViewModel : ViewModel() {
    private val initialBodies = SolarSystemScenario.create()
    private val engine = SimulationEngine(initialBodies)
    private var predictionTimer = 0.0

    val spawnPresets: List<SpawnPreset> = SolarSystemScenario.spawnPresets

    var uiState by mutableStateOf(OrbitUiState(snapshot = engine.snapshot()))
        private set

    fun onFrame(deltaSeconds: Double) {
        val clampedDelta = deltaSeconds.coerceIn(0.0, 0.08)
        if (uiState.isPlaying) {
            engine.stepSeconds(clampedDelta, uiState.timeScale)
        }
        predictionTimer -= clampedDelta
        refresh(recomputePrediction = uiState.showPrediction && predictionTimer <= 0.0)
    }

    fun togglePlay() {
        uiState = uiState.copy(isPlaying = !uiState.isPlaying)
    }

    fun setTimeScale(value: Double) {
        uiState = uiState.copy(timeScale = value.coerceIn(1.0, 220.0))
    }

    fun setTool(tool: GameTool) {
        uiState = uiState.copy(activeTool = tool)
    }

    fun zoomedOutSpeedPreset() {
        setTimeScale(120.0)
    }

    fun toggleTrails() {
        uiState = uiState.copy(showTrails = !uiState.showTrails)
    }

    fun togglePrediction() {
        val next = !uiState.showPrediction
        uiState = uiState.copy(
            showPrediction = next,
            predictionPaths = if (next) engine.predictTrajectories(uiState.selectedId) else emptyMap()
        )
        predictionTimer = 0.35
    }

    fun togglePalette() {
        uiState = uiState.copy(paletteOpen = !uiState.paletteOpen)
    }

    fun closePalette() {
        uiState = uiState.copy(paletteOpen = false)
    }

    fun reset() {
        engine.reset(initialBodies)
        uiState = OrbitUiState(snapshot = engine.snapshot())
        predictionTimer = 0.0
    }

    fun selectBody(id: Long?) {
        uiState = uiState.copy(selectedId = id)
        refresh(recomputePrediction = uiState.showPrediction)
    }

    fun selectNearest(world: Vector2, maxWorldDistance: Double) {
        selectBody(engine.nearestBody(world, maxWorldDistance)?.id)
    }

    fun spawnPresetAt(preset: SpawnPreset, world: Vector2) {
        val velocity = starterVelocity(world, preset)
        val id = engine.spawnPreset(preset, world, velocity)
        uiState = uiState.copy(selectedId = id ?: uiState.selectedId, paletteOpen = false)
        refresh(recomputePrediction = true)
    }

    fun moveSelected(world: Vector2) {
        val id = uiState.selectedId ?: return
        engine.moveBody(id, world)
        refresh(recomputePrediction = true)
    }

    fun setSelectedVelocityFromHandle(handleWorld: Vector2, handleScaleDays: Double) {
        val body = uiState.selectedBody ?: return
        val velocity = (handleWorld - body.position) / handleScaleDays
        engine.setVelocity(body.id, velocity)
        refresh(recomputePrediction = true)
    }

    fun nudgeSelectedVelocity(multiplier: Double) {
        val body = uiState.selectedBody ?: return
        val direction = if (body.velocity.length > 0.0) body.velocity.normalized() else Vector2(0.0, 1.0)
        engine.applyImpulse(body.id, direction * (0.0015 * multiplier))
        refresh(recomputePrediction = true)
    }

    fun circularizeSelected() {
        val id = uiState.selectedId ?: return
        engine.circularizeAroundPrimary(id)
        refresh(recomputePrediction = true)
    }

    fun toggleSelectedLock() {
        val id = uiState.selectedId ?: return
        engine.toggleLocked(id)
        refresh(recomputePrediction = true)
    }

    fun scaleSelectedMass(factor: Double) {
        val id = uiState.selectedId ?: return
        engine.updateMass(id, factor)
        refresh(recomputePrediction = true)
    }

    fun deleteSelected() {
        val id = uiState.selectedId ?: return
        engine.deleteBody(id)
        uiState = uiState.copy(selectedId = null)
        refresh(recomputePrediction = true)
    }

    private fun refresh(recomputePrediction: Boolean = false) {
        val snapshot = engine.snapshot()
        val selectedId = uiState.selectedId?.takeIf { id -> snapshot.bodies.any { it.id == id } }
        val prediction = when {
            !uiState.showPrediction -> emptyMap()
            recomputePrediction -> engine.predictTrajectories(selectedId)
            else -> uiState.predictionPaths
        }
        if (recomputePrediction) predictionTimer = 0.35
        uiState = uiState.copy(
            snapshot = snapshot,
            selectedId = selectedId,
            predictionPaths = prediction
        )
    }

    private fun starterVelocity(position: Vector2, preset: SpawnPreset): Vector2 {
        val primary = engine.snapshot().bodies.maxByOrNull { it.mass } ?: return Vector2.ZERO
        val offset = position - primary.position
        val radius = offset.length
        if (radius < 0.08) return Vector2(preset.defaultSpeed, 0.0)
        val speed = sqrt(2.9591220828559093e-4 * primary.mass / radius) + preset.defaultSpeed * 0.15
        val tangent = offset.normalized().perpendicularLeft()
        val typeKick = when (preset.type) {
            BodyType.Comet -> 0.55
            BodyType.Probe -> 1.25
            else -> 1.0
        }
        return primary.velocity + tangent * speed * typeKick
    }
}

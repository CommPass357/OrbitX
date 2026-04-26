package com.commpass357.orbitx.ui

import com.commpass357.orbitx.sim.Vector2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitViewModelTest {
    @Test
    fun playPauseAndTimeScaleUpdateState() {
        val viewModel = OrbitViewModel()

        viewModel.togglePlay()
        viewModel.setTimeScale(144.0)

        assertFalse(viewModel.uiState.isPlaying)
        assertEquals(144.0, viewModel.uiState.timeScale, 0.0)
    }

    @Test
    fun spawnSelectEditAndDeleteBody() {
        val viewModel = OrbitViewModel()
        val initialCount = viewModel.uiState.snapshot.bodies.size

        viewModel.spawnPresetAt(viewModel.spawnPresets.first(), Vector2(1.4, 0.2))
        val spawned = viewModel.uiState.selectedBody
        assertNotNull(spawned)

        viewModel.moveSelected(Vector2(1.6, -0.1))
        assertEquals(Vector2(1.6, -0.1), viewModel.uiState.selectedBody!!.position)

        viewModel.deleteSelected()
        assertEquals(initialCount, viewModel.uiState.snapshot.bodies.size)
    }

    @Test
    fun resetRestoresInitialScenario() {
        val viewModel = OrbitViewModel()
        val initialCount = viewModel.uiState.snapshot.bodies.size

        viewModel.spawnPresetAt(viewModel.spawnPresets.last(), Vector2(3.0, 0.0))
        assertTrue(viewModel.uiState.snapshot.bodies.size > initialCount)

        viewModel.reset()

        assertEquals(initialCount, viewModel.uiState.snapshot.bodies.size)
        assertEquals(null, viewModel.uiState.selectedId)
    }

    @Test
    fun predictionToggleControlsPaths() {
        val viewModel = OrbitViewModel()

        viewModel.togglePrediction()
        assertTrue(viewModel.uiState.predictionPaths.isEmpty())

        viewModel.togglePrediction()
        assertTrue(viewModel.uiState.predictionPaths.isNotEmpty())
    }
}

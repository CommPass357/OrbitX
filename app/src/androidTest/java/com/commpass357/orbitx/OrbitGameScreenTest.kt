package com.commpass357.orbitx

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class OrbitGameScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainGameControlsAreVisible() {
        composeRule.onNodeWithText("OrbitX").assertIsDisplayed()
        composeRule.onNodeWithText("Navigate").assertIsDisplayed()
        composeRule.onNodeWithText("Spawn").assertIsDisplayed()
    }
}

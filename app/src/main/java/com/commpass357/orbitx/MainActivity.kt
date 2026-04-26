package com.commpass357.orbitx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.commpass357.orbitx.ui.OrbitGameScreen
import com.commpass357.orbitx.ui.theme.OrbitXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitXTheme {
                OrbitGameScreen()
            }
        }
    }
}

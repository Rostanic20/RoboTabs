package com.robo.robotabs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.robo.robotabs.player.TabScreen
import com.robo.robotabs.ui.theme.RoboTabsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoboTabsTheme {
                TabScreen()
            }
        }
    }
}

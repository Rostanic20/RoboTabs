package com.robo.robotabs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.robo.robotabs.library.LibraryScreen
import com.robo.robotabs.player.TabScreen
import com.robo.robotabs.ui.theme.RoboTabsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoboTabsTheme {
                var openSongId by rememberSaveable { mutableStateOf<String?>(null) }
                val songId = openSongId
                if (songId == null) {
                    LibraryScreen(onOpenSong = { openSongId = it })
                } else {
                    BackHandler { openSongId = null }
                    TabScreen(songId = songId, onBack = { openSongId = null })
                }
            }
        }
    }
}

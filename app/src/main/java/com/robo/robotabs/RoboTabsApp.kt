package com.robo.robotabs

import android.app.Application
import com.robo.robotabs.library.SongLibrary
import com.robo.robotabs.player.readSongInfo
import java.io.File

class RoboTabsApp : Application() {
    val library by lazy {
        SongLibrary(File(filesDir, "songs"), ::readSongInfo, untitled = getString(R.string.untitled))
    }
}

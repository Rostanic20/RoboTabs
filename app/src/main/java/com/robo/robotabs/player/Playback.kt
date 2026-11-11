package com.robo.robotabs.player

import alphaTab.AlphaTabApiBase
import alphaTab.AlphaTabView
import alphaTab.synth.PlayerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Playback {
    private var api: AlphaTabApiBase<AlphaTabView>? = null

    var ready by mutableStateOf(false)
        private set
    var playing by mutableStateOf(false)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var looping by mutableStateOf(false)
        private set

    fun attach(api: AlphaTabApiBase<AlphaTabView>) {
        this.api = api
        api.playbackSpeed = speed.toDouble()
        api.isLooping = looping
        api.playerReady.on { ready = true }
        api.playerStateChanged.on { playing = it.state == PlayerState.Playing }
    }

    fun playPause() {
        api?.playPause()
    }

    fun stop() {
        api?.stop()
    }

    fun changeSpeed(value: Float) {
        speed = value
        api?.playbackSpeed = value.toDouble()
    }

    fun changeLooping(value: Boolean) {
        looping = value
        api?.isLooping = value
    }
}

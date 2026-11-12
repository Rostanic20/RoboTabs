package com.robo.robotabs.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface Player {
    fun play(): Boolean
    fun pause()
    fun stop()
    fun setSpeed(value: Double)
    fun setLooping(value: Boolean)
}

class Playback(private val focus: AudioFocus) {
    private var player: Player? = null

    var ready by mutableStateOf(false)
        private set
    var playing by mutableStateOf(false)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var looping by mutableStateOf(false)
        private set

    fun attach(player: Player) {
        this.player = player
        player.setSpeed(speed.toDouble())
        player.setLooping(looping)
    }

    fun onReady() {
        ready = true
    }

    fun onPlayingChanged(value: Boolean) {
        playing = value
        if (!value || player == null) {
            focus.release()
        } else if (!focus.acquire(::pause)) {
            player?.pause()
        }
    }

    fun playPause() {
        if (playing) pause() else play()
    }

    private fun play() {
        val player = player ?: return
        if (focus.acquire(::pause) && !player.play()) focus.release()
    }

    fun pause() {
        player?.pause()
        focus.release()
    }

    fun stop() {
        player?.stop()
        focus.release()
    }

    fun release() {
        player = null
        focus.release()
    }

    fun changeSpeed(value: Float) {
        speed = value
        player?.setSpeed(value.toDouble())
    }

    fun changeLooping(value: Boolean) {
        looping = value
        player?.setLooping(value)
    }
}

package com.robo.robotabs.player

import alphaTab.AlphaTabApiBase
import alphaTab.AlphaTabView
import alphaTab.synth.PlayerState

fun Playback.attach(api: AlphaTabApiBase<AlphaTabView>) {
    attach(object : Player {
        override fun play() = api.play()
        override fun pause() = api.pause()
        override fun stop() = api.stop()
        override fun setSpeed(value: Double) {
            api.playbackSpeed = value
        }
        override fun setLooping(value: Boolean) {
            api.isLooping = value
        }
    })
    api.playerReady.on { onReady() }
    api.playerStateChanged.on { onPlayingChanged(it.state == PlayerState.Playing) }
}

package com.robo.robotabs.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTest {
    private val focus = FakeFocus()
    private val player = FakePlayer()
    private val playback = Playback(focus).also { it.attach(player) }

    @Test
    fun playTakesFocusFirst() {
        playback.playPause()

        assertEquals(1, player.plays)
        assertTrue(focus.held)
    }

    @Test
    fun refusedFocusDoesNotPlay() {
        focus.grant = false

        playback.playPause()

        assertEquals(0, player.plays)
        assertFalse(focus.held)
    }

    @Test
    fun playerThatCannotStartGivesFocusBack() {
        player.canPlay = false

        playback.playPause()

        assertFalse(focus.held)
    }

    @Test
    fun losingFocusPausesAndGivesItBack() {
        playback.playPause()
        playback.onPlayingChanged(true)

        focus.lose()

        assertEquals(1, player.pauses)
        assertFalse(focus.held)
    }

    @Test
    fun pauseGivesFocusBack() {
        playback.playPause()
        playback.onPlayingChanged(true)

        playback.playPause()

        assertEquals(1, player.pauses)
        assertFalse(focus.held)
    }

    @Test
    fun stopGivesFocusBack() {
        playback.playPause()
        playback.onPlayingChanged(true)
        assertTrue(focus.held)

        playback.stop()

        assertEquals(1, player.stops)
        assertFalse(focus.held)
    }

    @Test
    fun songEndingOnItsOwnGivesFocusBack() {
        playback.playPause()
        playback.onPlayingChanged(true)

        playback.onPlayingChanged(false)

        assertFalse(focus.held)
    }

    @Test
    fun lateStoppedEventAfterPlayIsRecoveredByThePlayingEvent() {
        playback.playPause()
        playback.onPlayingChanged(false)
        assertFalse(focus.held)

        playback.onPlayingChanged(true)

        assertTrue(focus.held)
        assertEquals(0, player.pauses)
    }

    @Test
    fun playingWithoutFocusIsPaused() {
        focus.grant = false

        playback.onPlayingChanged(true)

        assertEquals(1, player.pauses)
    }

    @Test
    fun leavingTheScreenGivesFocusBack() {
        playback.playPause()

        playback.release()
        focus.lose()

        assertFalse(focus.held)
        assertEquals(0, player.pauses)
    }

    @Test
    fun playingEventAfterLeavingTheScreenDoesNotTakeFocus() {
        playback.release()

        playback.onPlayingChanged(true)

        assertFalse(focus.held)
    }

    private class FakeFocus : AudioFocus {
        var grant = true
        private var onLost: (() -> Unit)? = null
        val held get() = onLost != null

        override fun acquire(onLost: () -> Unit): Boolean {
            if (!grant) return false
            this.onLost = onLost
            return true
        }

        override fun release() {
            onLost = null
        }

        fun lose() {
            onLost?.invoke()
        }
    }

    private class FakePlayer : Player {
        var canPlay = true
        var plays = 0
        var pauses = 0
        var stops = 0

        override fun play(): Boolean {
            plays++
            return canPlay
        }

        override fun pause() {
            pauses++
        }

        override fun stop() {
            stops++
        }

        override fun setSpeed(value: Double) = Unit
        override fun setLooping(value: Boolean) = Unit
    }
}

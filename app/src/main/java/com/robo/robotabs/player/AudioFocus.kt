package com.robo.robotabs.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

interface AudioFocus {
    fun acquire(onLost: () -> Unit): Boolean
    fun release()
}

class SystemAudioFocus(private val context: Context) : AudioFocus {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var onLost: (() -> Unit)? = null

    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener(
            { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    onLost?.invoke()
                }
            },
            Handler(Looper.getMainLooper()),
        )
        .build()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onLost?.invoke()
        }
    }

    override fun acquire(onLost: () -> Unit): Boolean {
        if (this.onLost == null) {
            if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
            ContextCompat.registerReceiver(
                context,
                noisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        this.onLost = onLost
        return true
    }

    override fun release() {
        if (onLost == null) return
        onLost = null
        audioManager.abandonAudioFocusRequest(request)
        context.unregisterReceiver(noisyReceiver)
    }
}

package com.robo.robotabs.player

import alphaTab.AlphaTabError
import alphaTab.core.ecmaScript.Uint8Array
import alphaTab.importer.ScoreLoader
import alphaTab.model.Score
import com.robo.robotabs.library.SongInfo

fun parseScore(bytes: ByteArray): Score? =
    try {
        ScoreLoader.loadScoreFromBytes(Uint8Array(bytes.asUByteArray()))
    } catch (e: Exception) {
        null
    } catch (e: AlphaTabError) {
        null
    }

fun readSongInfo(bytes: ByteArray): SongInfo? =
    parseScore(bytes)?.let { SongInfo(title = it.title.trim(), artist = it.artist.trim()) }

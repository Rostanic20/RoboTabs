package com.robo.robotabs.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class ScoreParserTest {
    private val sample = File("../samples/two-tracks.atex").readBytes()

    @Test
    fun parsesSampleTab() {
        val score = parseScore(sample)!!

        assertEquals("RoboTabs test", score.title)
        assertEquals(listOf("Lead", "Rhythm"), score.tracks.toList().map { it.name })
        assertEquals(4.0, score.masterBars.length, 0.0)
    }

    @Test
    fun fileThatIsNotATabGivesNullInsteadOfThrowing() {
        val zip = byteArrayOf(0x50, 0x4B, 0x03, 0x04) + ByteArray(64)

        assertNull(parseScore(zip))
    }

    @Test
    fun songInfoComesFromTheScore() {
        val info = readSongInfo(sample)!!

        assertEquals("RoboTabs test", info.title)
        assertEquals("", info.artist)
    }
}

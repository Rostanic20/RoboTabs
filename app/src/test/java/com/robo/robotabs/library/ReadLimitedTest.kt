package com.robo.robotabs.library

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadLimitedTest {
    private val bytes = ByteArray(20_000) { it.toByte() }

    @Test
    fun streamUnderTheLimitIsReadWhole() {
        assertArrayEquals(bytes, bytes.inputStream().readAtMost(bytes.size + 1))
    }

    @Test
    fun streamExactlyAtTheLimitIsReadWhole() {
        assertArrayEquals(bytes, bytes.inputStream().readAtMost(bytes.size))
    }

    @Test
    fun streamOverTheLimitIsRejected() {
        assertNull(bytes.inputStream().readAtMost(bytes.size - 1))
    }
}

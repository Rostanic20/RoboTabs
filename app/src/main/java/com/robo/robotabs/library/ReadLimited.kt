package com.robo.robotabs.library

import java.io.ByteArrayOutputStream
import java.io.InputStream

fun InputStream.readAtMost(limit: Int): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = read(buffer)
        if (read < 0) return out.toByteArray()
        if (out.size() + read > limit) return null
        out.write(buffer, 0, read)
    }
}

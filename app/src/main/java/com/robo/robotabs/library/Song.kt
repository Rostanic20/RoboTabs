package com.robo.robotabs.library

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val addedAt: Long,
)

data class SongInfo(
    val title: String,
    val artist: String,
)

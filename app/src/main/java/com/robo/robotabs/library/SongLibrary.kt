package com.robo.robotabs.library

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class SongLibrary(
    private val dir: File,
    private val readInfo: (ByteArray) -> SongInfo?,
    private val untitled: String = "Untitled",
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val indexFile = File(dir, "index.json")

    private val _songs = MutableStateFlow<List<Song>?>(null)
    val songs: StateFlow<List<Song>?> = _songs.asStateFlow()

    suspend fun load() {
        withContext(io) {
            mutex.withLock { loadedSongs() }
        }
    }

    suspend fun add(bytes: ByteArray, fallbackTitle: String): Song? = withContext(io) {
        val info = readInfo(bytes) ?: return@withContext null
        val id = sha256(bytes)
        mutex.withLock {
            val current = loadedSongs()
            val existing = current.firstOrNull { it.id == id }
            if (existing != null && songFile(id).isFile) return@withLock existing
            dir.mkdirs()
            writeAtomically(songFile(id), bytes)
            if (existing != null) return@withLock existing
            val song = Song(id, info.title.ifBlank { fallbackTitle.ifBlank { untitled } }, info.artist, clock())
            save(current + song)
            song
        }
    }

    suspend fun remove(id: String) {
        withContext(io) {
            mutex.withLock {
                songFile(id).delete()
                save(loadedSongs().filterNot { it.id == id })
            }
        }
    }

    suspend fun read(id: String): ByteArray? = withContext(io) {
        try {
            songFile(id).takeIf { it.isFile }?.readBytes()
        } catch (e: IOException) {
            null
        }
    }

    private fun loadedSongs(): List<Song> =
        _songs.value ?: readIndex().also { _songs.value = it }

    private fun save(songs: List<Song>) {
        writeAtomically(indexFile, toJson(songs).toByteArray())
        _songs.value = songs
    }

    private fun songFile(id: String) = File(dir, "$id$TAB_SUFFIX")

    private fun readIndex(): List<Song> {
        val indexed = parseIndex()
        val files = dir.listFiles { file -> file.isFile && file.name.endsWith(TAB_SUFFIX) }
            .orEmpty()
            .associateBy { it.name.removeSuffix(TAB_SUFFIX) }
        val kept = indexed.orEmpty().filter { it.id in files }
        val keptIds = kept.mapTo(HashSet()) { it.id }
        val recovered = files
            .filterKeys { it !in keptIds }
            .mapNotNull { (id, file) -> recover(id, file) }
        val songs = kept + recovered
        if (songs != indexed && (indexFile.exists() || songs.isNotEmpty())) {
            writeAtomically(indexFile, toJson(songs).toByteArray())
        }
        return songs
    }

    private fun parseIndex(): List<Song>? {
        if (!indexFile.isFile) return null
        return try {
            val array = JSONArray(indexFile.readText())
            List(array.length()) { i ->
                val item = array.getJSONObject(i)
                Song(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    artist = item.getString("artist"),
                    addedAt = item.getLong("addedAt"),
                )
            }
        } catch (e: JSONException) {
            null
        }
    }

    private fun recover(id: String, file: File): Song? {
        val bytes = try {
            file.readBytes()
        } catch (e: IOException) {
            return null
        }
        val info = readInfo(bytes) ?: return null
        return Song(id, info.title.ifBlank { untitled }, info.artist, file.lastModified())
    }

    private fun toJson(songs: List<Song>): String {
        val array = JSONArray()
        songs.forEach { song ->
            array.put(
                JSONObject()
                    .put("id", song.id)
                    .put("title", song.title)
                    .put("artist", song.artist)
                    .put("addedAt", song.addedAt)
            )
        }
        return array.toString()
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val temp = File(target.path + ".tmp")
        FileOutputStream(temp).use {
            it.write(bytes)
            it.fd.sync()
        }
        Files.move(
            temp.toPath(),
            target.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAB_SUFFIX = ".tab"
    }
}

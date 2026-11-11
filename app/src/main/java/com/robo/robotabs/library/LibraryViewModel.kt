package com.robo.robotabs.library

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.robo.robotabs.RoboTabsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface LibraryEvent {
    data class Open(val songId: String) : LibraryEvent
    data object ImportFailed : LibraryEvent
    data object TooLarge : LibraryEvent
    data object DeleteFailed : LibraryEvent
}

private sealed interface PickedFile {
    class Tab(val bytes: ByteArray, val name: String) : PickedFile
    data object TooLarge : PickedFile
    data object Unreadable : PickedFile
}

class LibraryViewModel(
    private val library: SongLibrary,
    private val contentResolver: ContentResolver,
) : ViewModel() {
    val songs: StateFlow<List<Song>?> = library.songs
        .map { songs -> songs?.sortedBy { it.title.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events: Flow<LibraryEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { library.load() }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            _importing.value = true
            val event = when (val picked = withContext(Dispatchers.IO) { readFile(uri) }) {
                is PickedFile.Tab -> addToLibrary(picked)
                PickedFile.TooLarge -> LibraryEvent.TooLarge
                PickedFile.Unreadable -> LibraryEvent.ImportFailed
            }
            _importing.value = false
            _events.send(event)
        }
    }

    private suspend fun addToLibrary(picked: PickedFile.Tab): LibraryEvent =
        try {
            library.add(picked.bytes, picked.name)
                ?.let { LibraryEvent.Open(it.id) }
                ?: LibraryEvent.ImportFailed
        } catch (e: IOException) {
            LibraryEvent.ImportFailed
        }

    fun delete(songId: String) {
        viewModelScope.launch {
            try {
                library.remove(songId)
            } catch (e: IOException) {
                _events.send(LibraryEvent.DeleteFailed)
            }
        }
    }

    private fun readFile(uri: Uri): PickedFile {
        val stream = try {
            contentResolver.openInputStream(uri)
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } ?: return PickedFile.Unreadable
        val bytes = try {
            stream.use { it.readAtMost(MAX_TAB_BYTES) } ?: return PickedFile.TooLarge
        } catch (e: IOException) {
            return PickedFile.Unreadable
        }
        return PickedFile.Tab(bytes, displayName(uri))
    }

    private fun displayName(uri: Uri): String {
        val name = try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } catch (e: RuntimeException) {
            null
        }
        return name?.substringBeforeLast('.').orEmpty()
    }

    companion object {
        private const val MAX_TAB_BYTES = 20 * 1024 * 1024

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RoboTabsApp
                LibraryViewModel(app.library, app.contentResolver)
            }
        }
    }
}

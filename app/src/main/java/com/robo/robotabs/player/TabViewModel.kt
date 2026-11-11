package com.robo.robotabs.player

import alphaTab.AlphaTabError
import alphaTab.core.ecmaScript.Uint8Array
import alphaTab.importer.ScoreLoader
import alphaTab.model.Score
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TabUiState(
    val score: Score? = null,
    val trackIndex: Int = 0,
    val loading: Boolean = false,
)

class TabViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(TabUiState())
    val state: StateFlow<TabUiState> = _state.asStateFlow()

    private val _loadFailed = Channel<Unit>(Channel.CONFLATED)
    val loadFailed: Flow<Unit> = _loadFailed.receiveAsFlow()

    private var loadJob: Job? = null

    fun open(uri: Uri) {
        loadJob?.cancel()
        _state.update { it.copy(loading = true) }
        loadJob = viewModelScope.launch {
            val score = withContext(Dispatchers.IO) { readScore(uri) }
            if (score == null) {
                _state.update { it.copy(loading = false) }
                _loadFailed.send(Unit)
            } else {
                _state.value = TabUiState(score = score)
            }
        }
    }

    fun selectTrack(index: Int) {
        _state.update { it.copy(trackIndex = index) }
    }

    private fun readScore(uri: Uri): Score? {
        return try {
            val bytes = getApplication<Application>().contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: return null
            ScoreLoader.loadScoreFromBytes(Uint8Array(bytes.asUByteArray()))
        } catch (e: Exception) {
            null
        } catch (e: AlphaTabError) {
            null
        }
    }
}

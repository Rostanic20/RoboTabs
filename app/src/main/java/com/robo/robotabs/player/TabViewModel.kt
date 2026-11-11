package com.robo.robotabs.player

import alphaTab.model.Score
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.robo.robotabs.RoboTabsApp
import com.robo.robotabs.library.SongLibrary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TabUiState(
    val songId: String? = null,
    val score: Score? = null,
    val trackIndex: Int = 0,
    val loading: Boolean = false,
    val loadFailed: Boolean = false,
)

class TabViewModel(
    private val library: SongLibrary,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(TabUiState())
    val state: StateFlow<TabUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun open(songId: String) {
        if (_state.value.songId == songId && !_state.value.loadFailed) return
        loadJob?.cancel()
        _state.value = TabUiState(songId = songId, loading = true)
        loadJob = viewModelScope.launch {
            val score = library.read(songId)?.let { withContext(parseDispatcher) { parseScore(it) } }
            _state.value = TabUiState(songId = songId, score = score, loadFailed = score == null)
        }
    }

    fun selectTrack(index: Int) {
        _state.update { it.copy(trackIndex = index) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { TabViewModel((this[APPLICATION_KEY] as RoboTabsApp).library) }
        }
    }
}

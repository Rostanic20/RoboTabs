package com.robo.robotabs.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robo.robotabs.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenSong: (String) -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val importFailedMessage = stringResource(R.string.load_failed)
    val tooLargeMessage = stringResource(R.string.file_too_large)
    val deleteFailedMessage = stringResource(R.string.delete_failed)
    val currentOnOpenSong by rememberUpdatedState(onOpenSong)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.Open -> currentOnOpenSong(event.songId)
                LibraryEvent.ImportFailed -> snackbarHostState.showSnackbar(importFailedMessage)
                LibraryEvent.TooLarge -> snackbarHostState.showSnackbar(tooLargeMessage)
                LibraryEvent.DeleteFailed -> snackbarHostState.showSnackbar(deleteFailedMessage)
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }
    val addSong = { picker.launch(arrayOf("*/*")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = addSong, enabled = !importing) {
                        Icon(Icons.Default.Add, stringResource(R.string.add_song))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            val list = songs
            when {
                list == null -> Unit
                list.isEmpty() -> EmptyLibrary(addSong, enabled = !importing, Modifier.weight(1f))
                else -> SongList(list, onOpenSong, onDelete = { pendingDeleteId = it.id }, Modifier.weight(1f))
            }
        }
    }

    songs?.firstOrNull { it.id == pendingDeleteId }?.let { song ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.delete_song_title, song.title)) },
            text = { Text(stringResource(R.string.delete_song_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(song.id)
                    pendingDeleteId = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    onOpen: (String) -> Unit,
    onDelete: (Song) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            ListItem(
                headlineContent = { Text(song.title) },
                supportingContent = song.artist.takeIf { it.isNotBlank() }?.let { { Text(it) } },
                trailingContent = {
                    IconButton(onClick = { onDelete(song) }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete))
                    }
                },
                modifier = Modifier.clickable { onOpen(song.id) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyLibrary(onAdd: () -> Unit, enabled: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.library_empty), textAlign = TextAlign.Center)
        Button(onClick = onAdd, enabled = enabled) { Text(stringResource(R.string.add_song)) }
    }
}

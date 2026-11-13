package com.robo.robotabs.player

import alphaTab.AlphaTabView
import alphaTab.model.Score
import alphaTab.model.Track
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robo.robotabs.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScreen(
    songId: String,
    onBack: () -> Unit,
    viewModel: TabViewModel = viewModel(factory = TabViewModel.Factory),
) {
    LaunchedEffect(songId) { viewModel.open(songId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current.applicationContext
    val playback = remember { Playback(SystemAudioFocus(context)) }
    DisposableEffect(playback) { onDispose { playback.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { playback.pause() }
    val view = LocalView.current
    DisposableEffect(playback.playing) {
        view.keepScreenOn = playback.playing
        onDispose { view.keepScreenOn = false }
    }
    val score = state.score.takeIf { state.songId == songId }
    val tracks = remember(score) { score?.tracks?.toList().orEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = score?.title?.takeIf { it.isNotBlank() }.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (tracks.size > 1) {
                        TrackPicker(tracks, state.trackIndex, viewModel::selectTrack)
                    }
                },
            )
        },
        bottomBar = {
            if (score != null) PlayerControls(playback)
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                score != null -> TabSheet(score, state.trackIndex, playback, Modifier.weight(1f))
                state.loadFailed -> Text(
                    text = stringResource(R.string.song_load_failed),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
                else -> LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TabSheet(score: Score, trackIndex: Int, playback: Playback, modifier: Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize().background(Color.White),
        factory = { context ->
            AlphaTabView(context, null).also { view ->
                view.applyFinalSizeOnRenderFinished()
                playback.attach(view.api)
            }
        },
        update = { view ->
            val track = score.tracks[trackIndex]
            if (view.tracks?.firstOrNull() !== track) {
                playback.stop()
                view.doOnLayout { view.tracks = listOf(track) }
            }
        },
    )
}

@Composable
private fun PlayerControls(playback: Playback) {
    BottomAppBar {
        IconButton(onClick = playback::playPause, enabled = playback.ready) {
            if (playback.playing) {
                Icon(painterResource(R.drawable.ic_pause), stringResource(R.string.pause))
            } else {
                Icon(Icons.Default.PlayArrow, stringResource(R.string.play))
            }
        }
        IconButton(onClick = playback::stop, enabled = playback.ready) {
            Icon(painterResource(R.drawable.ic_stop), stringResource(R.string.stop))
        }
        IconToggleButton(checked = playback.looping, onCheckedChange = playback::changeLooping) {
            Icon(Icons.Default.Refresh, stringResource(R.string.loop))
        }
        Slider(
            value = playback.speed,
            onValueChange = playback::changeSpeed,
            valueRange = 0.25f..1.5f,
            steps = 4,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Text(
            text = stringResource(R.string.speed_value, (playback.speed * 100).roundToInt()),
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 48.dp).padding(end = 8.dp),
        )
    }
}

@Composable
private fun TrackPicker(tracks: List<Track>, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = tracks[selected].name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
            Icon(Icons.Default.ArrowDropDown, stringResource(R.string.choose_track))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tracks.forEachIndexed { index, track ->
                DropdownMenuItem(
                    text = { Text(track.name) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                )
            }
        }
    }
}

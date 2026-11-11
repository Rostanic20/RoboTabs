package com.robo.robotabs.player

import com.robo.robotabs.library.SongLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TabViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val sample = File("../samples/two-tracks.atex").readBytes()
    private val otherSample = String(sample).replace("RoboTabs test", "Second song").toByteArray()

    private lateinit var library: SongLibrary
    private lateinit var viewModel: TabViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        library = SongLibrary(folder.root, ::readSongInfo, io = dispatcher)
        viewModel = TabViewModel(library, parseDispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun opensASongFromTheLibrary() = runTest(dispatcher) {
        val song = library.add(sample, "sample")!!

        viewModel.open(song.id)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("RoboTabs test", state.score!!.title)
        assertFalse(state.loading)
        assertFalse(state.loadFailed)
    }

    @Test
    fun reopeningTheSameSongKeepsTheLoadedScore() = runTest(dispatcher) {
        val song = library.add(sample, "sample")!!
        viewModel.open(song.id)
        advanceUntilIdle()
        val first = viewModel.state.value.score

        viewModel.open(song.id)

        assertSame(first, viewModel.state.value.score)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun unknownSongFailsToLoad() = runTest(dispatcher) {
        viewModel.open("missing")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loadFailed)
        assertNull(viewModel.state.value.score)
    }

    @Test
    fun failedSongIsLoadedAgainOnTheNextOpen() = runTest(dispatcher) {
        val song = library.add(sample, "sample")!!
        File(folder.root, "${song.id}.tab").delete()
        viewModel.open(song.id)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.loadFailed)

        library.add(sample, "sample")
        viewModel.open(song.id)
        advanceUntilIdle()

        assertEquals("RoboTabs test", viewModel.state.value.score!!.title)
    }

    @Test
    fun openingAnotherSongResetsTheTrack() = runTest(dispatcher) {
        val first = library.add(sample, "first")!!
        val second = library.add(otherSample, "second")!!
        viewModel.open(first.id)
        advanceUntilIdle()
        viewModel.selectTrack(1)

        viewModel.open(second.id)
        advanceUntilIdle()

        assertEquals("Second song", viewModel.state.value.score!!.title)
        assertEquals(0, viewModel.state.value.trackIndex)
    }
}

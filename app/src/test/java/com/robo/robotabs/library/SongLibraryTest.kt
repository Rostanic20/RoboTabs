package com.robo.robotabs.library

import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SongLibraryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val tab = "tab:Wish You Were Here:Pink Floyd".toByteArray()
    private val untitledTab = "tab::".toByteArray()

    private fun readInfo(bytes: ByteArray): SongInfo? {
        val parts = String(bytes).split(":")
        if (parts.size != 3 || parts[0] != "tab") return null
        return SongInfo(parts[1], parts[2])
    }

    private fun TestScope.library(dir: File = folder.root) = SongLibrary(
        dir = dir,
        readInfo = ::readInfo,
        io = StandardTestDispatcher(testScheduler),
        clock = { 42L },
    )

    @Test
    fun addStoresSongAndItsBytes() = runTest {
        val library = library()

        val song = library.add(tab, "fallback")!!

        assertEquals("Wish You Were Here", song.title)
        assertEquals("Pink Floyd", song.artist)
        assertEquals(listOf(song), library.songs.value)
        assertArrayEquals(tab, library.read(song.id))
    }

    @Test
    fun songsSurviveANewLibraryInstance() = runTest {
        val song = library().add(tab, "fallback")!!

        val reopened = library()
        reopened.load()

        assertEquals(listOf(song), reopened.songs.value)
        assertArrayEquals(tab, reopened.read(song.id))
    }

    @Test
    fun addingTheSameFileTwiceKeepsOneSong() = runTest {
        val library = library()

        val first = library.add(tab, "first")
        val second = library.add(tab, "second")

        assertEquals(first, second)
        assertEquals(1, library.songs.value!!.size)
    }

    @Test
    fun blankTitleFallsBackToFileName() = runTest {
        val song = library().add(untitledTab, "my-riff")!!

        assertEquals("my-riff", song.title)
    }

    @Test
    fun fileThatIsNotATabIsRejectedAndNothingIsWritten() = runTest {
        val library = library()

        val song = library.add("PK zip bytes".toByteArray(), "archive")

        assertNull(song)
        assertEquals(emptyList<String>(), folder.root.list()!!.toList())
    }

    @Test
    fun removeDeletesSongAndFile() = runTest {
        val library = library()
        val song = library.add(tab, "fallback")!!

        library.remove(song.id)

        assertEquals(emptyList<Song>(), library.songs.value)
        assertNull(library.read(song.id))
        assertFalse(File(folder.root, "${song.id}.tab").exists())
        val reopened = library()
        reopened.load()
        assertEquals(emptyList<Song>(), reopened.songs.value)
    }

    @Test
    fun loadOnAnEmptyDirectoryGivesAnEmptyList() = runTest {
        val library = library(File(folder.root, "missing"))

        library.load()

        assertEquals(emptyList<Song>(), library.songs.value)
        assertFalse(File(folder.root, "missing").exists())
    }

    @Test
    fun corruptIndexIsRebuiltFromTabFiles() = runTest {
        val song = library().add(tab, "fallback")!!
        File(folder.root, "index.json").writeText("{ not json")

        val reopened = library()
        reopened.load()

        assertEquals(listOf(song.id), reopened.songs.value!!.map { it.id })
        assertEquals("Wish You Were Here", reopened.songs.value!!.single().title)
        val again = library()
        again.load()
        assertEquals(reopened.songs.value, again.songs.value)
    }

    @Test
    fun indexFromAnOlderFormatIsRebuiltInsteadOfCrashing() = runTest {
        val song = library().add(tab, "fallback")!!
        File(folder.root, "index.json").writeText("""[{"id":"${song.id}"}]""")

        val reopened = library()
        reopened.load()

        assertEquals(listOf(song.id), reopened.songs.value!!.map { it.id })
    }

    @Test
    fun lostIndexIsRebuiltFromTabFiles() = runTest {
        val song = library().add(tab, "fallback")!!
        File(folder.root, "index.json").delete()

        val reopened = library()
        reopened.load()

        assertEquals(listOf(song.id), reopened.songs.value!!.map { it.id })
        assertTrue(File(folder.root, "index.json").isFile)
    }

    @Test
    fun indexEntryWhoseFileIsGoneIsDropped() = runTest {
        val song = library().add(tab, "fallback")!!
        File(folder.root, "${song.id}.tab").delete()

        val reopened = library()
        reopened.load()

        assertEquals(emptyList<Song>(), reopened.songs.value)
    }

    @Test
    fun unreadableTabFileIsSkippedButNotDeleted() = runTest {
        val stray = File(folder.root, "stray.tab").apply { writeText("not a tab") }

        val library = library()
        library.load()

        assertEquals(emptyList<Song>(), library.songs.value)
        assertTrue(stray.exists())
    }

    @Test
    fun addingAgainRestoresAMissingFile() = runTest {
        val library = library()
        val song = library.add(tab, "fallback")!!
        File(folder.root, "${song.id}.tab").delete()

        val again = library.add(tab, "fallback")

        assertEquals(song, again)
        assertArrayEquals(tab, library.read(song.id))
        assertEquals(1, library.songs.value!!.size)
    }

    @Test
    fun blankTitleAndBlankFileNameGiveUntitled() = runTest {
        val song = library().add(untitledTab, "")!!

        assertEquals("Untitled", song.title)
    }

    @Test
    fun removingAnUnknownIdChangesNothing() = runTest {
        val library = library()
        val song = library.add(tab, "fallback")!!

        library.remove("unknown")

        assertEquals(listOf(song), library.songs.value)
    }

    @Test
    fun concurrentAddsKeepBothSongs() = runTest {
        val other = "tab:Hey Joe:Jimi Hendrix".toByteArray()
        val library = library()

        val first = async { library.add(tab, "a") }
        val second = async { library.add(other, "b") }
        val ids = setOf(first.await()!!.id, second.await()!!.id)

        val reopened = library()
        reopened.load()
        assertEquals(ids, reopened.songs.value!!.map { it.id }.toSet())
    }
}

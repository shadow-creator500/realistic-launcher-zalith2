/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.saves

import com.movtery.zalithlauncher.utils.file.ZipEntryBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SaveZipImportTest {

    @Test
    fun findsWorldAtZipRoot() {
        val path = findLevelEntryName(
            sequenceOf(
                Entry("level.dat"),
                Entry("region/", isDirectory = true),
                Entry("region/r.0.0.mca")
            )
        )

        assertEquals("", path)
    }

    @Test
    fun acceptsMinimalWorldAtZipRoot() {
        val path = findLevelEntryName(
            sequenceOf(Entry("level.dat"))
        )

        assertEquals("", path)
    }

    @Test
    fun acceptsMinimalWorldInsideSingleFolder() {
        val path = findLevelEntryName(
            sequenceOf(Entry("MyWorld/level.dat"))
        )

        assertEquals("MyWorld", path)
    }

    @Test
    fun findsWorldInsideSingleFolder() {
        val path = findLevelEntryName(
            sequenceOf(
                Entry("MyWorld/", isDirectory = true),
                Entry("MyWorld/level.dat"),
                Entry("MyWorld/region/", isDirectory = true),
                Entry("MyWorld/region/r.0.0.mca")
            )
        )

        assertEquals("MyWorld", path)
    }

    @Test
    fun ignoresExtraFilesBesideWorldFolder() {
        val path = findLevelEntryName(
            sequenceOf(
                Entry("README.txt"),
                Entry("preview/cover.png"),
                Entry("World/level.dat"),
                Entry("World/region/", isDirectory = true),
                Entry("World/region/r.0.0.mca")
            )
        )

        assertEquals("World", path)
    }

    @Test
    fun findsWorldInsideNestedExportFolders() {
        val path = findLevelEntryName(
            sequenceOf(
                Entry("download/"),
                Entry("download/export/"),
                Entry("download/export/World/level.dat"),
                Entry("download/export/World/region/", isDirectory = true),
                Entry("download/export/World/region/r.0.0.mca"),
                Entry("download/README.txt")
            )
        )

        assertEquals("download/export/World", path)
    }

    @Test
    fun rejectsArchivesContainingMultipleWorlds() {
        val path = findLevelEntryName(
            sequenceOf(
                Entry("WorldA/level.dat"),
                Entry("WorldA/region/", isDirectory = true),
                Entry("WorldB/level.dat"),
                Entry("WorldB/region/", isDirectory = true)
            )
        )

        assertNull(path)
    }

    @Test
    fun rejectsArchivesWithoutLevelDat() {
        val path = findLevelEntryName(
            sequenceOf(
                Entry("README.txt"),
                Entry("region/", isDirectory = true),
                Entry("region/r.0.0.mca")
            )
        )

        assertNull(path)
    }

    @Test
    fun reservesNewDirectoryWithoutOverwritingExistingWorld() {
        val root = Files.createTempDirectory("realistic-save-import").toFile()
        try {
            val existing = File(root, "World")
            assertTrue(existing.mkdir())
            File(existing, "keep.txt").writeText("keep")

            val imported = createUniqueSaveDirectory(root, "World")

            assertEquals("World (2)", imported.name)
            assertTrue(imported.isDirectory)
            assertEquals("keep", File(existing, "keep.txt").readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun sanitizesImportedWorldFolderName() {
        val root = Files.createTempDirectory("realistic-save-name").toFile()
        try {
            val imported = createUniqueSaveDirectory(root, "unsafe/name")

            assertEquals("unsafe_name", imported.name)
            assertTrue(imported.isDirectory)
        } finally {
            root.deleteRecursively()
        }
    }

    private data class Entry(
        override val name: String,
        override val isDirectory: Boolean = false
    ) : ZipEntryBase
}

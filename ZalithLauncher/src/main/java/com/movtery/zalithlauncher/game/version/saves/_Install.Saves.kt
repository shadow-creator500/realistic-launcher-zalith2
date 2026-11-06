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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.saves

import com.movtery.zalithlauncher.utils.file.CompressZipEntryAdapter
import com.movtery.zalithlauncher.utils.file.JavaZipEntryAdapter
import com.movtery.zalithlauncher.utils.file.UnpackZipException
import com.movtery.zalithlauncher.utils.file.ZipEntryBase
import com.movtery.zalithlauncher.utils.file.extractFromZip
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile as JDKZipFile

private const val TAG = "InstallSaves"

/**
 * 解压存档压缩包
 */
suspend fun unpackSaveZip(zipFile: File, targetPath: File) = withContext(Dispatchers.IO) {
    var target: File? = null
    try {
        val path = extractLevelPath(zipFile)
            ?: throw IOException("Unable to locate the level where the level.dat file is stored.")
        Logger.info(TAG, "Found the level of the level.data file: $path")

        if (!targetPath.exists() && !targetPath.mkdirs()) {
            throw IOException("Unable to create the worlds directory: $targetPath")
        }
        if (!targetPath.isDirectory) {
            throw IOException("The worlds path is not a directory: $targetPath")
        }

        // Atomically reserve a unique destination so concurrent imports cannot overwrite each other.
        target = createUniqueSaveDirectory(targetPath, zipFile.nameWithoutExtension)

        try {
            JDKZipFile(zipFile).use { zip ->
                zip.extractFromZip(path, target!!)
                Logger.info(TAG, "Decompression is complete")
            }
        } catch (e: Exception) {
            if (e !is UnpackZipException) {
                tryApacheZip(zipFile, path, target!!)
            } else {
                throw e
            }
        }

        // A successful unzip is not enough: Minecraft worlds must have level.dat at the root.
        if (!File(target!!, "level.dat").isFile) {
            throw IOException("The imported archive does not contain level.dat at the world root.")
        }

        FileUtils.deleteQuietly(zipFile)
    } catch (e: Exception) {
        target?.let { FileUtils.deleteQuietly(it) }
        // The ZIP is a temporary copy inside the saves directory; don't leave broken imports there.
        FileUtils.deleteQuietly(zipFile)
        throw e
    }
}
internal fun createUniqueSaveDirectory(targetPath: File, archiveBaseName: String): File {
    val baseName = archiveBaseName
        .replace(Regex("""[\\/:*?"<>|\t\n]"""), "_")
        .replace("..", "_")
        .trim()
        .take(100)
        .ifBlank { "Imported World" }
        .let { if (it == "." || it == "..") "Imported World" else it }

    var suffix = 1
    while (true) {
        val folderName = if (suffix == 1) baseName else "$baseName ($suffix)"
        val candidate = File(targetPath, folderName)

        // mkdir() is atomic on the destination filesystem: only one import can claim this name.
        if (candidate.mkdir()) return candidate
        if (!candidate.exists()) {
            throw IOException("Unable to reserve the world extraction directory: $candidate")
        }
        suffix++
    }
}

private suspend fun tryApacheZip(zipFile: File, path: String, target: File) {
    // Keep the destination directory reserved while removing a partial first extraction.
    if (target.isDirectory) {
        FileUtils.cleanDirectory(target)
    } else if (!target.mkdirs()) {
        throw IOException("Unable to prepare the world extraction directory: $target")
    }
    val zipFile1 = ZipFile.Builder()
        .setFile(zipFile)
        .get()

    zipFile1.use { zip ->
        zip.extractFromZip(path, target)
        Logger.info(TAG, "Decompression is complete")
    }
}

/**
 * 读取zip文件，并找到level.data文件所在的路径
 * @param file 压缩包文件
 */
private fun extractLevelPath(file: File): String? {
    if (!file.exists() || !file.isFile) {
        return null
    }

    if (!file.name.endsWith(".zip", ignoreCase = true)) {
        return null
    }

    try {
        JDKZipFile(file).use { zip ->
            val entries = zip.entries().asSequence()
                .map { entry ->
                    JavaZipEntryAdapter(entry)
                }
            return findLevelEntryName(entries)
        }
    } catch (e: Exception) {
        if (e !is UnpackZipException) {
            return extractLevelPathCompress(file)
        } else {
            throw e
        }
    }
}

private fun extractLevelPathCompress(file: File): String? {
    val zipFile = ZipFile.Builder()
        .setFile(file)
        .get()

    zipFile.use { zip ->
        val entries = zip.entries.asSequence()
            .map { entry ->
                CompressZipEntryAdapter(entry)
            }
        return findLevelEntryName(entries)
    }
}

internal fun <T : ZipEntryBase> findLevelEntryName(
    entries: Sequence<T>
): String? {
    // level.dat is the reliable world marker. Do not require region/ or data/
    // because a newly-created or deliberately minimal world can contain only this file.
    val levelDatPaths = entries
        .filter { entry ->
            !entry.isDirectory &&
                entry.name.substringAfterLast('/') == "level.dat"
        }
        .map { it.name }
        .toList()

    // Prefer a world stored directly at the ZIP root.
    if (levelDatPaths.any { it == "level.dat" }) {
        return ""
    }

    // Ignore unrelated files beside the world folder, but reject archives
    // containing multiple possible worlds rather than importing the wrong one.
    val candidates = levelDatPaths.map { path ->
        path.substringBeforeLast('/', missingDelimiterValue = "")
    }.filter { it.isNotBlank() }
        .distinct()

    return candidates.singleOrNull()
}

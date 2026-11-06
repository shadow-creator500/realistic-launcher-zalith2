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

        // Never extract over an existing world. Give repeated imports a distinct folder.
        val baseName = zipFile.nameWithoutExtension
            .replace(Regex("""[\\/:*?"<>|\t\n]"""), "_")
            .replace("..", "_")
            .trim()
            .ifBlank { "Imported World" }
        target = File(targetPath, baseName)
        var suffix = 2
        while (target!!.exists()) {
            target = File(targetPath, "$baseName (${suffix})")
            suffix++
        }

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
private suspend fun tryApacheZip(zipFile: File, path: String, target: File) {
    FileUtils.deleteQuietly(target) //清除一次目标文件夹（如果之前解压出错）
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
    val allEntries = entries.toList()

    fun hasWorldContents(prefix: String): Boolean {
        return allEntries.any { entry ->
            if (entry.name == prefix || !entry.name.startsWith(prefix)) {
                false
            } else {
                val relative = entry.name.removePrefix(prefix)
                entry.isDirectory || relative.contains("/")
            }
        }
    }

    val levelDatEntries = allEntries.filter { entry ->
        !entry.isDirectory && entry.name.substringAfterLast('/')
            .equals("level.dat", ignoreCase = true)
    }

    // Prefer a world stored directly at the ZIP root.
    if (levelDatEntries.any { it.name.equals("level.dat", ignoreCase = true) } &&
        hasWorldContents("")
    ) {
        return ""
    }

    // Ignore unrelated root files (such as README.txt) and accept exactly one world folder.
    val candidates = levelDatEntries.map { entry ->
        entry.name.substringBeforeLast('/', missingDelimiterValue = "")
            .takeIf { it.isNotEmpty() }
            ?.let { "$it/" }
    }.filterNotNull()
        .distinct()
        .filter(::hasWorldContents)

    return candidates.singleOrNull()?.removeSuffix("/")
}

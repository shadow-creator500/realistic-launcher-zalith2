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

package com.movtery.zalithlauncher.utils.file

import org.apache.commons.codec.digest.MurmurHash2
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.random.Random

class MurmurHash2IncrementalTest {

    @Test
    fun testTwoWay() {
        val file = Files.createTempFile("murmur2-legacy", ".jar").toFile()
        try {
            file.writeText("sample mod metadata\nwith whitespace\tand bytes")
            assertEquals(way1(file), way2(file))
        } finally {
            file.delete()
        }
    }

    //Old
    private fun way1(file: File): Long {
        val baos = ByteArrayOutputStream()
        Files.newInputStream(file.toPath()).use { stream ->
            val buf = ByteArray(1024)
            var bytesRead: Int
            while (stream.read(buf).also { bytesRead = it } != -1) {
                for (i in 0 until bytesRead) {
                    val b = buf[i]
                    if (b.toInt() !in listOf(0x9, 0xa, 0xd, 0x20)) {
                        baos.write(b.toInt())
                    }
                }
            }
        }
        return Integer.toUnsignedLong(MurmurHash2.hash32(baos.toByteArray(), baos.size(), 1))
    }

    private fun way2(file: File): Long {
        return MurmurHash2Incremental.computeHash(file, byteToSkip = listOf(0x9, 0xa, 0xd, 0x20))
    }

    /**
     * 参考实现：过滤字节后交由 commons-codec 整体哈希
     */
    private fun referenceHash(file: File, byteToSkip: List<Int>): Long {
        val baos = ByteArrayOutputStream()
        Files.newInputStream(file.toPath()).use { stream ->
            val buf = ByteArray(1024)
            var bytesRead: Int
            while (stream.read(buf).also { bytesRead = it } != -1) {
                for (i in 0 until bytesRead) {
                    val b = buf[i]
                    if (b.toInt() !in byteToSkip) {
                        baos.write(b.toInt())
                    }
                }
            }
        }
        return Integer.toUnsignedLong(MurmurHash2.hash32(baos.toByteArray(), baos.size(), 1))
    }

    @Test
    fun computeHashMatchesReference() {
        val byteToSkip = listOf(0x9, 0xa, 0xd, 0x20)
        val random = Random(seed = 42)
        //覆盖空文件、块边界前后、以及大量需要剔除的字节等场景
        val sizes = intArrayOf(0, 1, 3, 4, 5, 8191, 8192, 8193, 100_003)

        val dir = createTempDirectory("murmur2-test").toFile()
        try {
            for (size in sizes) {
                val bytes = ByteArray(size) {
                    when (random.nextInt(4)) {
                        0 -> byteToSkip[random.nextInt(byteToSkip.size)]
                        else -> random.nextInt(256)
                    }.toByte()
                }
                val file = dir.resolve("mod-$size.jar")
                Files.write(file.toPath(), bytes)

                val expected = referenceHash(file, byteToSkip)
                assertEquals(
                    "incremental hash mismatch for size $size",
                    expected,
                    MurmurHash2Incremental.computeHash(file, byteToSkip = byteToSkip)
                )
                assertEquals(
                    "precomputed length hash mismatch for size $size",
                    expected,
                    MurmurHash2Incremental.computeHash(
                        file,
                        byteToSkip = byteToSkip,
                        filteredLength = bytes.count { it.toInt() !in byteToSkip }
                    )
                )
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
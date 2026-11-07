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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class FileTest {

    @Test
    fun testCalculateFileSha1() {
        val file = Files.createTempFile("sha1-test", ".jar").toFile()
        try {
            file.writeText("REALISTIC LAUNCHER SHA-1 fixture")
            val expected = MessageDigest.getInstance("SHA-1")
                .digest(file.readBytes())
                .joinToString("") { "%02x".format(it) }
            val actual = runBlocking(Dispatchers.IO) { calculateFileSha1(file) }
            assertEquals(expected, actual)
        } finally {
            file.delete()
        }
    }
}
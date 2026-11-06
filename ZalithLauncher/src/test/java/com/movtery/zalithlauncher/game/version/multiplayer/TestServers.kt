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

package com.movtery.zalithlauncher.game.version.multiplayer

import com.movtery.zalithlauncher.utils.network.ServerAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic address parsing tests. Live server pings belong in manual integration tests,
 * not the default unit-test suite, because CI must not depend on public network availability.
 */
class TestServers {

    @Test
    fun parsesAddressWithDefaultPort() {
        val address = ServerAddress.parse("play.example.net")

        assertEquals("play.example.net", address.host)
        assertEquals(ServerAddress.DEFAULT_PORT, address.port)
    }

    @Test
    fun parsesAddressWithExplicitPort() {
        val address = ServerAddress.parse("play.example.net:25570")

        assertEquals("play.example.net", address.host)
        assertEquals(25570, address.port)
    }

    @Test
    fun parsesBracketedIpv6Address() {
        val address = ServerAddress.parse("[2001:db8::1]:25575")

        assertEquals("2001:db8::1", address.host)
        assertEquals(25575, address.port)
    }

    @Test
    fun rejectsInvalidPortsAndEmptyAddresses() {
        assertTrue(runCatching { ServerAddress.parse("") }.isFailure)
        assertTrue(runCatching { ServerAddress.parse("play.example.net:abc") }.isFailure)
        assertTrue(runCatching { ServerAddress.parse("play.example.net:65536") }.isFailure)
    }
}

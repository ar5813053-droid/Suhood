package com.suhood

import com.suhood.feature.focus.SalahLockLogic
import com.suhood.feature.focus.SalahLockSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SalahLockLogicTest {

    @Test
    fun start_setsExpiryExactly10Minutes() {
        val now = 1_700_000_000_000L
        val snap = SalahLockLogic.start(now, 1000L, "Dhuhr", setOf("com.example.game"))
        assertTrue(snap.active)
        assertEquals(now + SalahLockLogic.DURATION_MS, snap.expiryWallMs)
        assertEquals(10 * 60 * 1000L, SalahLockLogic.DURATION_MS)
    }

    @Test
    fun remaining_decreases_notFixed() {
        val now = 1_000_000L
        val snap = SalahLockLogic.start(now, 0L, "Asr", setOf("com.x"))
        assertEquals(10 * 60 * 1000L, SalahLockLogic.remainingMs(snap, now))
        assertEquals(9 * 60 * 1000L, SalahLockLogic.remainingMs(snap, now + 60_000L))
        assertEquals(0L, SalahLockLogic.remainingMs(snap, now + SalahLockLogic.DURATION_MS))
    }

    @Test
    fun formatMmSs() {
        assertEquals("10:00", SalahLockLogic.formatMmSs(10 * 60 * 1000L))
        assertEquals("09:58", SalahLockLogic.formatMmSs(9 * 60 * 1000L + 58_000L))
        assertEquals("00:00", SalahLockLogic.formatMmSs(0))
    }

    @Test
    fun buttonDoesNotClear_logicStillActive() {
        // Simulates Exit App: state unchanged
        val now = 5_000_000L
        val snap = SalahLockLogic.start(now, 0L, "Maghrib", setOf("com.blocked"))
        // User "exits" — we do not call clear; still active 5 min later
        assertTrue(SalahLockLogic.isActive(snap, now + 5 * 60 * 1000L))
        assertTrue(SalahLockLogic.shouldBlockPackage(snap, now + 5 * 60 * 1000L, "com.blocked", "com.suhood.app"))
    }

    @Test
    fun reopenBlockedApp_stillBlockedBeforeExpiry() {
        val now = 10_000L
        val snap = SalahLockLogic.start(now, 0L, "Isha", setOf("com.social"))
        assertTrue(SalahLockLogic.shouldBlockPackage(snap, now + 30_000L, "com.social", "com.suhood.app"))
    }

    @Test
    fun suhoodItselfNeverBlocked() {
        val now = 10_000L
        val snap = SalahLockLogic.start(now, 0L, "Fajr", setOf("com.suhood.app", "com.other"))
        assertFalse(SalahLockLogic.shouldBlockPackage(snap, now, "com.suhood.app", "com.suhood.app"))
    }

    @Test
    fun expiryRestoresAccess() {
        val now = 100L
        val snap = SalahLockLogic.start(now, 0L, "Fajr", setOf("com.x"))
        val after = now + SalahLockLogic.DURATION_MS
        assertFalse(SalahLockLogic.isActive(snap, after))
        assertFalse(SalahLockLogic.shouldBlockPackage(snap, after, "com.x", "com.suhood.app"))
    }

    @Test
    fun reconcileClearsExpired() {
        val now = 1000L
        val snap = SalahLockLogic.start(now, 0L, "Fajr", setOf("com.x"))
        val cleared = SalahLockLogic.reconcile(snap, now + SalahLockLogic.DURATION_MS + 1)
        assertFalse(cleared.active)
    }

    @Test
    fun emptyPackages_notActive() {
        val snap = SalahLockLogic.start(0L, 0L, "Fajr", emptySet())
        assertFalse(snap.active)
        assertFalse(SalahLockLogic.isActive(snap, 0L))
    }

    @Test
    fun processRecreation_preservesExpiry() {
        // Same snapshot object would be re-read from prefs; remaining uses wall clock
        val start = 1_000_000L
        val snap = SalahLockLogic.start(start, 50L, "Dhuhr", setOf("com.a"))
        val recreated = snap.copy() // same fields after process death read
        assertEquals(
            SalahLockLogic.remainingMs(snap, start + 90_000L),
            SalahLockLogic.remainingMs(recreated, start + 90_000L)
        )
    }
}

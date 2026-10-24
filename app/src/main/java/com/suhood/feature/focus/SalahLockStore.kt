package com.suhood.feature.focus

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistent Salah app-restriction state.
 *
 * Wall-clock timestamps are the source of truth across process death / reboot.
 * [elapsedRealtimeAtStart] helps detect large clock jumps during an active session.
 */
data class SalahLockSnapshot(
    val active: Boolean,
    val prayerName: String,
    val startWallMs: Long,
    val expiryWallMs: Long,
    val elapsedRealtimeAtStart: Long,
    val blockedPackages: Set<String>
)

object SalahLockLogic {
    const val DURATION_MS = 10 * 60 * 1000L

    fun start(
        nowWallMs: Long,
        elapsedRealtimeMs: Long,
        prayerName: String,
        packages: Set<String>
    ): SalahLockSnapshot = SalahLockSnapshot(
        active = packages.isNotEmpty(),
        prayerName = prayerName,
        startWallMs = nowWallMs,
        expiryWallMs = nowWallMs + DURATION_MS,
        elapsedRealtimeAtStart = elapsedRealtimeMs,
        blockedPackages = packages
    )

    fun remainingMs(snap: SalahLockSnapshot, nowWallMs: Long): Long {
        if (!snap.active) return 0L
        return (snap.expiryWallMs - nowWallMs).coerceAtLeast(0L)
    }

    fun isActive(snap: SalahLockSnapshot, nowWallMs: Long): Boolean {
        if (!snap.active) return false
        if (snap.blockedPackages.isEmpty()) return false
        return nowWallMs < snap.expiryWallMs
    }

    /** Clear if expired or packages empty. */
    fun reconcile(snap: SalahLockSnapshot, nowWallMs: Long): SalahLockSnapshot {
        return if (isActive(snap, nowWallMs)) snap
        else snap.copy(active = false, blockedPackages = emptySet())
    }

    fun formatMmSs(remainingMs: Long): String {
        val totalSec = (remainingMs / 1000L).coerceAtLeast(0L)
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    fun shouldBlockPackage(
        snap: SalahLockSnapshot,
        nowWallMs: Long,
        packageName: String,
        suhoodPackage: String
    ): Boolean {
        if (packageName == suhoodPackage) return false
        if (!isActive(snap, nowWallMs)) return false
        return packageName in snap.blockedPackages
    }
}

class SalahLockStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): SalahLockSnapshot {
        val pkgs = prefs.getString(KEY_PKGS, "")
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            .orEmpty()
        return SalahLockSnapshot(
            active = prefs.getBoolean(KEY_ACTIVE, false),
            prayerName = prefs.getString(KEY_PRAYER, "") ?: "",
            startWallMs = prefs.getLong(KEY_START, 0L),
            expiryWallMs = prefs.getLong(KEY_EXPIRY, 0L),
            elapsedRealtimeAtStart = prefs.getLong(KEY_ELAPSED, 0L),
            blockedPackages = pkgs
        )
    }

    fun write(snap: SalahLockSnapshot) {
        prefs.edit()
            .putBoolean(KEY_ACTIVE, snap.active)
            .putString(KEY_PRAYER, snap.prayerName)
            .putLong(KEY_START, snap.startWallMs)
            .putLong(KEY_EXPIRY, snap.expiryWallMs)
            .putLong(KEY_ELAPSED, snap.elapsedRealtimeAtStart)
            .putString(KEY_PKGS, snap.blockedPackages.joinToString(","))
            .commit()
    }

    fun activate(prayerName: String, packages: Set<String>, nowWallMs: Long, elapsedRealtimeMs: Long) {
        if (packages.isEmpty()) return
        // Do not extend an already-active window — keep original expiry
        val cur = reconcileNow(nowWallMs)
        if (SalahLockLogic.isActive(cur, nowWallMs)) {
            // Merge packages if needed, keep expiry
            val merged = cur.blockedPackages + packages
            write(cur.copy(blockedPackages = merged, prayerName = prayerName.ifBlank { cur.prayerName }))
            return
        }
        write(SalahLockLogic.start(nowWallMs, elapsedRealtimeMs, prayerName, packages))
    }

    fun clear() {
        write(
            SalahLockSnapshot(
                active = false,
                prayerName = "",
                startWallMs = 0L,
                expiryWallMs = 0L,
                elapsedRealtimeAtStart = 0L,
                blockedPackages = emptySet()
            )
        )
    }

    fun reconcileNow(nowWallMs: Long = System.currentTimeMillis()): SalahLockSnapshot {
        val cur = read()
        val next = SalahLockLogic.reconcile(cur, nowWallMs)
        if (next.active != cur.active || next.blockedPackages != cur.blockedPackages) {
            write(next)
        }
        return next
    }

    companion object {
        private const val PREFS = "salah_lock_v1"
        private const val KEY_ACTIVE = "active"
        private const val KEY_PRAYER = "prayer"
        private const val KEY_START = "start"
        private const val KEY_EXPIRY = "expiry"
        private const val KEY_ELAPSED = "elapsed"
        private const val KEY_PKGS = "pkgs"

        @Volatile private var instance: SalahLockStore? = null
        fun get(context: Context): SalahLockStore =
            instance ?: synchronized(this) {
                instance ?: SalahLockStore(context.applicationContext).also { instance = it }
            }
    }
}

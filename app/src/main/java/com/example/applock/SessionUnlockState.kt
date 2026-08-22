package com.example.applock

/**
 * Tracks which locked packages have already been unlocked in the current
 * "session" so the user isn't re-prompted every time they briefly switch
 * away and back. Cleared automatically after a short timeout or when the
 * app is force-stopped/re-locked from Settings.
 */
object SessionUnlockState {
    private val unlockedAt = mutableMapOf<String, Long>()

    /** How long an unlock stays valid before the app is challenged again. */
    private const val SESSION_MS = 60_000L

    fun isUnlocked(packageName: String): Boolean {
        val ts = unlockedAt[packageName] ?: return false
        return System.currentTimeMillis() - ts < SESSION_MS
    }

    fun markUnlocked(packageName: String) {
        unlockedAt[packageName] = System.currentTimeMillis()
    }

    fun forget(packageName: String) {
        unlockedAt.remove(packageName)
    }

    fun clearAll() {
        unlockedAt.clear()
    }
}

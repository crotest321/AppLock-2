package com.example.applock

object SessionUnlockState {
    private val unlocked = mutableSetOf<String>()

    fun isUnlocked(packageName: String): Boolean = unlocked.contains(packageName)

    fun markUnlocked(packageName: String) {
        unlocked.add(packageName)
    }

    fun forget(packageName: String) {
        unlocked.remove(packageName)
    }

    fun clearAll() {
        unlocked.clear()
    }
}

cat > /home/claude/AppLock/app/src/main/java/com/example/applock/SessionUnlockState.kt << 'EOF'
package com.example.applock

/**
 * Tracks which locked packages are currently unlocked. An app stays
 * unlocked only while it remains the foreground app — the moment the
 * user switches away from it (home, recents, another app), the
 * AccessibilityService forgets its unlock, so returning to it always
 * asks for the credential again.
 */
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
EOF
echo done

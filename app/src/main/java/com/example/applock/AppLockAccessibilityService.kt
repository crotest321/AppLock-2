package com.example.applock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Enable this in Settings -> Accessibility -> App Lock. It only reads
 * *which package* is currently in the foreground (canRetrieveWindowContent
 * is off in accessibility_service_config.xml, so it never reads screen
 * content/text from other apps) and shows the lock challenge when a
 * locked app comes to the front — whether opened from the home screen,
 * Recents, or a notification.
 *
 * An app is considered "unlocked" only while it stays the foreground app.
 * The instant the user leaves it, its unlock is forgotten, so coming back
 * always asks for the credential again.
 */
class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var security: SecurityManager
    private var lastForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        security = SecurityManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == lastForegroundPackage) return

        val previous = lastForegroundPackage
        if (previous != null && previous != pkg) {
            if (!::security.isInitialized) security = SecurityManager(this)
            if (security.isLocked(previous)) {
                SessionUnlockState.forget(previous)
            }
        }

        lastForegroundPackage = pkg

        if (!::security.isInitialized) security = SecurityManager(this)

        if (pkg == packageName) return

        if (security.isLocked(pkg) && !SessionUnlockState.isUnlocked(pkg)) {
            val label = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                pkg
            }
            val lockIntent = Intent(this, LockActivity::class.java).apply {
                putExtra(LockActivity.EXTRA_PACKAGE_NAME, pkg)
                putExtra(LockActivity.EXTRA_APP_LABEL, label)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(lockIntent)
        }
    }

    override fun onInterrupt() {}
}

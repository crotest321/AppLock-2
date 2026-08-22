package com.example.applock

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Central store for everything security related. Nothing here ever touches
 * the network — it's all local, backed by EncryptedSharedPreferences
 * (AES-256, keys held in the Android Keystore).
 *
 * Two layers of credential:
 *  - a GLOBAL master credential (used by default for every locked app, and
 *    also to open this AppLock app itself)
 *  - an optional PER-APP custom credential that overrides the global one
 *    for a specific package — this is what gives "different app, different
 *    lock" support.
 */
class SecurityManager(context: Context) {

    enum class Method { NONE, PIN, PATTERN, PASSWORD }

    /** The special key used to represent "this AppLock app itself" as a lockable target. */
    val selfPackage: String = context.packageName

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "applock_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ---------- Global master credential ----------

    fun isGlobalCredentialSet(): Boolean = prefs.getString(KEY_G_HASH, null) != null

    fun getGlobalMethod(): Method =
        Method.valueOf(prefs.getString(KEY_G_METHOD, Method.NONE.name)!!)

    fun setGlobalCredential(method: Method, rawValue: String) {
        val salt = generateSalt()
        prefs.edit()
            .putString(KEY_G_METHOD, method.name)
            .putString(KEY_G_SALT, salt)
            .putString(KEY_G_HASH, hash(rawValue, salt))
            .apply()
    }

    fun verifyGlobalCredential(rawValue: String): Boolean {
        val salt = prefs.getString(KEY_G_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_G_HASH, null) ?: return false
        return hash(rawValue, salt) == storedHash
    }

    fun isGlobalBiometricEnabled(): Boolean = prefs.getBoolean(KEY_G_BIOMETRIC, false)

    fun setGlobalBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_G_BIOMETRIC, enabled).apply()
    }

    // ---------- Per-app custom credential ----------

    fun hasCustomCredential(packageName: String): Boolean =
        prefs.getString(customKey(packageName, "hash"), null) != null

    fun getCustomMethod(packageName: String): Method =
        Method.valueOf(prefs.getString(customKey(packageName, "method"), Method.NONE.name)!!)

    fun setCustomCredential(packageName: String, method: Method, rawValue: String) {
        val salt = generateSalt()
        prefs.edit()
            .putString(customKey(packageName, "method"), method.name)
            .putString(customKey(packageName, "salt"), salt)
            .putString(customKey(packageName, "hash"), hash(rawValue, salt))
            .apply()
    }

    fun clearCustomCredential(packageName: String) {
        prefs.edit()
            .remove(customKey(packageName, "method"))
            .remove(customKey(packageName, "salt"))
            .remove(customKey(packageName, "hash"))
            .apply()
    }

    fun verifyCustomCredential(packageName: String, rawValue: String): Boolean {
        val salt = prefs.getString(customKey(packageName, "salt"), null) ?: return false
        val storedHash = prefs.getString(customKey(packageName, "hash"), null) ?: return false
        return hash(rawValue, salt) == storedHash
    }

    fun isCustomBiometricEnabled(packageName: String): Boolean =
        prefs.getBoolean(customKey(packageName, "biometric"), false)

    fun setCustomBiometricEnabled(packageName: String, enabled: Boolean) {
        prefs.edit().putBoolean(customKey(packageName, "biometric"), enabled).apply()
    }

    /** Effective method for a given app: its own custom method if set, else the global one. */
    fun effectiveMethod(packageName: String): Method =
        if (hasCustomCredential(packageName)) getCustomMethod(packageName) else getGlobalMethod()

    fun effectiveBiometricEnabled(packageName: String): Boolean =
        if (hasCustomCredential(packageName)) isCustomBiometricEnabled(packageName) else isGlobalBiometricEnabled()

    fun verifyEffectiveCredential(packageName: String, rawValue: String): Boolean =
        if (hasCustomCredential(packageName)) verifyCustomCredential(packageName, rawValue)
        else verifyGlobalCredential(rawValue)

    // ---------- Locked apps ----------

    fun getLockedApps(): MutableSet<String> =
        HashSet(prefs.getStringSet(KEY_LOCKED, emptySet()) ?: emptySet())

    fun setLocked(packageName: String, locked: Boolean) {
        val set = getLockedApps()
        if (locked) set.add(packageName) else set.remove(packageName)
        prefs.edit().putStringSet(KEY_LOCKED, set).apply()
    }

    fun isLocked(packageName: String): Boolean =
        packageName == selfPackage || getLockedApps().contains(packageName)

    // ---------- Hidden apps (soft-hide, see README for why) ----------

    fun getHiddenApps(): MutableSet<String> =
        HashSet(prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet())

    fun setHidden(packageName: String, hidden: Boolean) {
        val set = getHiddenApps()
        if (hidden) set.add(packageName) else set.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN, set).apply()
    }

    fun isHidden(packageName: String): Boolean = getHiddenApps().contains(packageName)

    // ---------- Helpers ----------

    private fun customKey(packageName: String, field: String) = "custom_${field}_$packageName"

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hash(value: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray())
        val bytes = digest.digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_G_METHOD = "g_method"
        private const val KEY_G_SALT = "g_salt"
        private const val KEY_G_HASH = "g_hash"
        private const val KEY_G_BIOMETRIC = "g_biometric"
        private const val KEY_LOCKED = "locked_apps"
        private const val KEY_HIDDEN = "hidden_apps"
    }
}

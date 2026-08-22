package com.example.applock

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class LockActivity : AppCompatActivity() {

    private lateinit var security: SecurityManager
    private var targetPackage: String = ""
    private var isSelf: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)
        security = SecurityManager(this)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        isSelf = intent.getBooleanExtra(EXTRA_IS_SELF, false)
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: ""

        if (!isSelf && SessionUnlockState.isUnlocked(targetPackage)) {
            openTargetAndFinish()
            return
        }

        findViewById<TextView>(R.id.lockTitle).text = getString(R.string.unlock_title, appLabel)

        val entryField = findViewById<EditText>(R.id.lockEntryField)
        val confirmButton = findViewById<Button>(R.id.lockConfirmButton)
        val patternView = findViewById<PatternLockView>(R.id.lockPatternView)
        val errorText = findViewById<TextView>(R.id.lockError)
        val biometricButton = findViewById<Button>(R.id.biometricButton)

        when (security.effectiveMethod(targetPackage)) {
            SecurityManager.Method.PIN, SecurityManager.Method.PASSWORD -> {
                entryField.visibility = android.view.View.VISIBLE
                confirmButton.visibility = android.view.View.VISIBLE
                entryField.inputType = if (security.effectiveMethod(targetPackage) == SecurityManager.Method.PIN)
                    android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                else
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

                confirmButton.setOnClickListener {
                    if (security.verifyEffectiveCredential(targetPackage, entryField.text.toString())) {
                        onUnlockSuccess()
                    } else {
                        errorText.text = getString(R.string.wrong_credential)
                        entryField.text.clear()
                    }
                }
            }
            SecurityManager.Method.PATTERN -> {
                patternView.visibility = android.view.View.VISIBLE
                patternView.onPatternComplete = { pattern ->
                    if (security.verifyEffectiveCredential(targetPackage, pattern)) {
                        onUnlockSuccess()
                    } else {
                        errorText.text = getString(R.string.wrong_credential)
                    }
                }
            }
            SecurityManager.Method.NONE -> onUnlockSuccess()
        }

        if (security.effectiveBiometricEnabled(targetPackage) && canUseBiometrics()) {
            biometricButton.visibility = android.view.View.VISIBLE
            biometricButton.setOnClickListener { showBiometricPrompt() }
            showBiometricPrompt()
        }
    }

    private fun canUseBiometrics(): Boolean {
        val manager = BiometricManager.from(this)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlockSuccess()
            }
        })
        // BIOMETRIC_WEAK covers whatever the device offers — fingerprint, face, or both.
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.use_biometric))
            .setNegativeButtonText(getString(android.R.string.cancel))
            .build()
        prompt.authenticate(info)
    }

    private fun onUnlockSuccess() {
        if (isSelf) {
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            SessionUnlockState.markUnlocked(targetPackage)
            openTargetAndFinish()
        }
    }

    private fun openTargetAndFinish() {
        packageManager.getLaunchIntentForPackage(targetPackage)?.let { startActivity(it) }
        finish()
    }

    override fun onBackPressed() {
        if (isSelf) {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_IS_SELF = "extra_is_self"
    }
}

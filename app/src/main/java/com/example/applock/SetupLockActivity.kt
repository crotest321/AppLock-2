package com.example.applock

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SetupLockActivity : AppCompatActivity() {

    private lateinit var security: SecurityManager
    private var chosenMethod: SecurityManager.Method = SecurityManager.Method.NONE
    private var firstEntry: String? = null

    /** null => setting up the GLOBAL master lock. Non-null => custom lock for that one app. */
    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_lock)
        security = SecurityManager(this)

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        val targetLabel = intent.getStringExtra(EXTRA_TARGET_LABEL)

        val title = findViewById<TextView>(R.id.title)
        val methodChooser = findViewById<LinearLayout>(R.id.methodChooser)
        val textEntryContainer = findViewById<LinearLayout>(R.id.textEntryContainer)
        val patternEntryContainer = findViewById<LinearLayout>(R.id.patternEntryContainer)
        val entryLabel = findViewById<TextView>(R.id.entryLabel)
        val entryField = findViewById<EditText>(R.id.entryField)
        val entryError = findViewById<TextView>(R.id.entryError)
        val btnConfirmEntry = findViewById<Button>(R.id.btnConfirmEntry)
        val patternLabel = findViewById<TextView>(R.id.patternLabel)
        val patternView = findViewById<PatternLockView>(R.id.patternView)
        val patternError = findViewById<TextView>(R.id.patternError)
        val btnUseGlobal = findViewById<Button>(R.id.btnUseGlobal)

        if (targetPackage != null) {
            title.text = "Custom lock for ${targetLabel ?: targetPackage}"
            if (security.hasCustomCredential(targetPackage!!)) {
                btnUseGlobal.visibility = android.view.View.VISIBLE
                btnUseGlobal.setOnClickListener {
                    security.clearCustomCredential(targetPackage!!)
                    finish()
                }
            }
        }

        fun startTextFlow(method: SecurityManager.Method, isNumeric: Boolean) {
            chosenMethod = method
            firstEntry = null
            methodChooser.visibility = LinearLayout.GONE
            textEntryContainer.visibility = LinearLayout.VISIBLE
            entryField.inputType = if (isNumeric)
                android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            entryLabel.text = getString(if (method == SecurityManager.Method.PIN) R.string.enter_pin else R.string.enter_password)
        }

        findViewById<Button>(R.id.btnPin).setOnClickListener { startTextFlow(SecurityManager.Method.PIN, true) }
        findViewById<Button>(R.id.btnPassword).setOnClickListener { startTextFlow(SecurityManager.Method.PASSWORD, false) }
        findViewById<Button>(R.id.btnPattern).setOnClickListener {
            chosenMethod = SecurityManager.Method.PATTERN
            firstEntry = null
            methodChooser.visibility = LinearLayout.GONE
            patternEntryContainer.visibility = LinearLayout.VISIBLE
            patternLabel.text = getString(R.string.enter_pattern)
        }

        fun saveCredential(rawValue: String) {
            val pkg = targetPackage
            if (pkg == null) {
                security.setGlobalCredential(chosenMethod, rawValue)
            } else {
                security.setCustomCredential(pkg, chosenMethod, rawValue)
            }
            finish()
        }

        btnConfirmEntry.setOnClickListener {
            val value = entryField.text.toString()
            if (value.length < 4) {
                entryError.text = getString(R.string.wrong_credential)
                return@setOnClickListener
            }
            if (firstEntry == null) {
                firstEntry = value
                entryField.text.clear()
                entryLabel.text = getString(
                    if (chosenMethod == SecurityManager.Method.PIN) R.string.confirm_pin else R.string.confirm_password
                )
                entryError.text = ""
            } else if (firstEntry == value) {
                saveCredential(value)
            } else {
                entryError.text = getString(R.string.wrong_credential)
                firstEntry = null
                entryField.text.clear()
                entryLabel.text = getString(
                    if (chosenMethod == SecurityManager.Method.PIN) R.string.enter_pin else R.string.enter_password
                )
            }
        }

        patternView.onPatternComplete = { pattern ->
            if (pattern.split(",").size < 4) {
                patternError.text = getString(R.string.wrong_credential)
            } else if (firstEntry == null) {
                firstEntry = pattern
                patternLabel.text = getString(R.string.confirm_pattern)
                patternError.text = ""
            } else if (firstEntry == pattern) {
                saveCredential(pattern)
            } else {
                patternError.text = getString(R.string.wrong_credential)
                firstEntry = null
                patternLabel.text = getString(R.string.enter_pattern)
            }
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_TARGET_LABEL = "extra_target_label"
    }
}

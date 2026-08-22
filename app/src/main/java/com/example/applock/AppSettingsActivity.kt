package com.example.applock

import android.content.Intent
import android.os.Bundle
import android.widget.Switch
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager

class AppSettingsActivity : AppCompatActivity() {

    private lateinit var security: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        security = SecurityManager(this)

        findViewById<Button>(R.id.btnChangeCredential).setOnClickListener {
            startActivity(Intent(this, SetupLockActivity::class.java))
        }

        val biometricSwitch = findViewById<Switch>(R.id.switchBiometric)
        val biometricAvailable = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        biometricSwitch.isEnabled = biometricAvailable
        biometricSwitch.isChecked = security.isGlobalBiometricEnabled()
        biometricSwitch.setOnCheckedChangeListener { _, checked ->
            security.setGlobalBiometricEnabled(checked)
        }
    }
}

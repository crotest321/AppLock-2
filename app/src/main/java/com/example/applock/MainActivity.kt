package com.example.applock

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var security: SecurityManager
    private lateinit var adapter: AppRowAdapter
    private var allApps: List<AppInfo> = emptyList()
    private var showHidden = false
    private var awaitingSelfUnlock = false

    private val selfUnlockLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        awaitingSelfUnlock = false
        if (result.resultCode == Activity.RESULT_OK) {
            SessionUnlockState.markUnlocked(security.selfPackage)
            loadApps()
        } else {
            // User canceled the self-unlock challenge — close the app.
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        security = SecurityManager(this)

        if (!security.isGlobalCredentialSet()) {
            startActivity(Intent(this, SetupLockActivity::class.java))
        }

        val list = findViewById<RecyclerView>(R.id.appList)
        list.layoutManager = LinearLayoutManager(this)
        adapter = AppRowAdapter(
            emptyList(),
            security,
            onCustomLockClick = { app ->
                val intent = Intent(this, SetupLockActivity::class.java).apply {
                    putExtra(SetupLockActivity.EXTRA_TARGET_PACKAGE, app.packageName)
                    putExtra(SetupLockActivity.EXTRA_TARGET_LABEL, app.label)
                }
                startActivity(intent)
            },
            onChanged = { loadApps() }
        )
        list.adapter = adapter

        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, AppSettingsActivity::class.java))
        }

        findViewById<EditText>(R.id.searchBox).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<CheckBox>(R.id.showHiddenCheck).setOnCheckedChangeListener { _, checked ->
            showHidden = checked
            loadApps()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!security.isGlobalCredentialSet()) return

        if (!SessionUnlockState.isUnlocked(security.selfPackage) && !awaitingSelfUnlock) {
            awaitingSelfUnlock = true
            val intent = Intent(this, LockActivity::class.java).apply {
                putExtra(LockActivity.EXTRA_PACKAGE_NAME, security.selfPackage)
                putExtra(LockActivity.EXTRA_APP_LABEL, getString(R.string.app_name))
                putExtra(LockActivity.EXTRA_IS_SELF, true)
            }
            selfUnlockLauncher.launch(intent)
            return
        }
        loadApps()
    }

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        val hidden = security.getHiddenApps()

        allApps = resolveInfos
            .filter { it.activityInfo.packageName != packageName }
            .filter { showHidden || it.activityInfo.packageName !in hidden }
            .map {
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            .sortedBy { it.label.lowercase() }

        adapter.updateList(allApps)
    }

    private fun filterApps(query: String) {
        if (query.isBlank()) {
            adapter.updateList(allApps)
        } else {
            adapter.updateList(allApps.filter { it.label.contains(query, ignoreCase = true) })
        }
    }
}

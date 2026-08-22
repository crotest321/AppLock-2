package com.example.applock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppRowAdapter(
    private var apps: List<AppInfo>,
    private val security: SecurityManager,
    private val onCustomLockClick: (AppInfo) -> Unit,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<AppRowAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.rowIcon)
        val label: TextView = view.findViewById(R.id.rowLabel)
        val subtitle: TextView = view.findViewById(R.id.rowSubtitle)
        val lockCheck: CheckBox = view.findViewById(R.id.rowLockCheck)
        val hideCheck: CheckBox = view.findViewById(R.id.rowHideCheck)
        val customButton: Button = view.findViewById(R.id.rowCustomLockButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_row, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label

        holder.lockCheck.setOnCheckedChangeListener(null)
        holder.hideCheck.setOnCheckedChangeListener(null)

        val locked = security.isLocked(app.packageName)
        val hidden = security.isHidden(app.packageName)
        holder.lockCheck.isChecked = locked
        holder.hideCheck.isChecked = hidden
        holder.customButton.visibility = if (locked) View.VISIBLE else View.GONE
        updateSubtitle(holder, app.packageName)

        holder.lockCheck.setOnCheckedChangeListener { _, checked ->
            security.setLocked(app.packageName, checked)
            holder.customButton.visibility = if (checked) View.VISIBLE else View.GONE
            updateSubtitle(holder, app.packageName)
            onChanged()
        }
        holder.hideCheck.setOnCheckedChangeListener { _, checked ->
            security.setHidden(app.packageName, checked)
            onChanged()
        }
        holder.customButton.setOnClickListener { onCustomLockClick(app) }
    }

    private fun updateSubtitle(holder: VH, packageName: String) {
        val method = security.effectiveMethod(packageName)
        val isCustom = security.hasCustomCredential(packageName)
        val label = when (method) {
            SecurityManager.Method.PIN -> holder.itemView.context.getString(R.string.pin)
            SecurityManager.Method.PATTERN -> holder.itemView.context.getString(R.string.pattern)
            SecurityManager.Method.PASSWORD -> holder.itemView.context.getString(R.string.password)
            SecurityManager.Method.NONE -> "-"
        }
        holder.subtitle.text = if (isCustom) "Custom: $label" else "Master lock ($label)"
    }

    override fun getItemCount() = apps.size

    fun updateList(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}

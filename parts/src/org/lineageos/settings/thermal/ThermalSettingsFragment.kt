/**
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.thermal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.Spinner
import android.widget.TextView

import androidx.preference.PreferenceFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.android.settingslib.applications.ApplicationsState

import org.lineageos.settings.R

import java.util.Arrays

class ThermalSettingsFragment : PreferenceFragment(), ApplicationsState.Callbacks {

    private lateinit var allPackagesAdapter: AllPackagesAdapter
    private lateinit var applicationsState: ApplicationsState
    private lateinit var session: ApplicationsState.Session
    private lateinit var activityFilter: ActivityFilter
    private val entryMap = mutableMapOf<String, ApplicationsState.AppEntry>()

    private lateinit var thermalUtils: ThermalUtils
    private lateinit var appsRecyclerView: RecyclerView

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applicationsState = ApplicationsState.getInstance(activity.application)
        session = applicationsState.newSession(this)
        session.onResume()
        activityFilter = ActivityFilter(activity.packageManager)

        allPackagesAdapter = AllPackagesAdapter(activity)

        thermalUtils = ThermalUtils(activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.thermal_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appsRecyclerView = view.findViewById(R.id.thermal_rv_view)
        appsRecyclerView.layoutManager = LinearLayoutManager(activity)
        appsRecyclerView.adapter = allPackagesAdapter
    }

    override fun setDivider(divider: Drawable?) {
        val list = listView
        if (list == null) {
            view?.post { setDivider(divider) }
            return
        }
        super.setDivider(divider)
    }

    override fun onResume() {
        super.onResume()
        activity.setTitle(resources.getString(R.string.thermal_title))
        rebuild()
    }

    override fun onDestroy() {
        super.onDestroy()
        session.onPause()
        session.onDestroy()
    }

    override fun onPackageListChanged() {
        activityFilter.updateLauncherInfoList()
        rebuild()
    }

    override fun onRebuildComplete(entries: ArrayList<ApplicationsState.AppEntry>?) {
        entries?.let {
            handleAppEntries(it)
            allPackagesAdapter.notifyDataSetChanged()
        }
    }

    override fun onLoadEntriesCompleted() = rebuild()
    override fun onAllSizesComputed() {}
    override fun onLauncherInfoChanged() {}
    override fun onPackageIconChanged() {}
    override fun onPackageSizeChanged(packageName: String?) {}
    override fun onRunningStateChanged(running: Boolean) {}

    private fun handleAppEntries(entries: List<ApplicationsState.AppEntry>) {
        val sections = mutableListOf<String>()
        val positions = mutableListOf<Int>()
        val pm = activity.packageManager
        var lastSectionIndex: String? = null
        var offset = 0

        for (entry in entries) {
            val info = entry.info
            val label = info.loadLabel(pm).toString()
            val sectionIndex = when {
                !info.enabled -> "--"
                label.isEmpty() -> ""
                else -> label.substring(0, 1).uppercase()
            }

            if (lastSectionIndex == null || sectionIndex != lastSectionIndex) {
                sections.add(sectionIndex)
                positions.add(offset)
                lastSectionIndex = sectionIndex
            }
            offset++
        }

        allPackagesAdapter.setEntries(entries, sections, positions)
        entryMap.clear()
        entries.forEach { entryMap[it.info.packageName] = it }
    }

    private fun rebuild() {
        session.rebuild(activityFilter, ApplicationsState.ALPHA_COMPARATOR)
    }

    private fun getStateDrawable(state: Int): Int = when (state) {
        ThermalUtils.STATE_BENCHMARK -> R.drawable.ic_thermal_benchmark
        ThermalUtils.STATE_BROWSER -> R.drawable.ic_thermal_browser
        ThermalUtils.STATE_CAMERA -> R.drawable.ic_thermal_camera
        ThermalUtils.STATE_DIALER -> R.drawable.ic_thermal_dialer
        ThermalUtils.STATE_GAMING -> R.drawable.ic_thermal_gaming
        ThermalUtils.STATE_NAVIGATION -> R.drawable.ic_thermal_navigation
        ThermalUtils.STATE_STREAMING -> R.drawable.ic_thermal_streaming
        ThermalUtils.STATE_VIDEO -> R.drawable.ic_thermal_video
        else -> R.drawable.ic_thermal_default
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.app_name)
        val mode: Spinner = view.findViewById(R.id.app_mode)
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val stateIcon: ImageView = view.findViewById(R.id.state)

        init {
            view.tag = this
        }
    }

    private inner class ModeAdapter(context: Context) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)
        private val items = intArrayOf(
            R.string.thermal_default,
            R.string.thermal_benchmark,
            R.string.thermal_browser,
            R.string.thermal_camera,
            R.string.thermal_dialer,
            R.string.thermal_gaming,
            R.string.thermal_navigation,
            R.string.thermal_streaming,
            R.string.thermal_video
        )

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = 0

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = (convertView as? TextView) ?: inflater.inflate(
                android.R.layout.simple_spinner_dropdown_item,
                parent,
                false
            ) as TextView

            view.setText(items[position])
            view.textSize = 14f
            return view
        }
    }

    private inner class AllPackagesAdapter(context: Context) :
        RecyclerView.Adapter<ViewHolder>(),
        AdapterView.OnItemSelectedListener,
        SectionIndexer {

        var entries: List<ApplicationsState.AppEntry> = emptyList()
            private set
        private var sections: Array<String> = emptyArray()
        private var positions: IntArray = IntArray(0)

        init {
            activityFilter = ActivityFilter(context.packageManager)
        }

        override fun getItemCount(): Int = entries.size
        override fun getItemId(position: Int): Long = entries[position].id.toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val holder = ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.thermal_list_item, parent, false)
            )
            val context = holder.itemView.context
            holder.mode.adapter = ModeAdapter(context)
            holder.mode.onItemSelectedListener = this
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position] ?: return

            holder.title.text = entry.label
            holder.title.setOnClickListener { holder.mode.performClick() }
            applicationsState.ensureIcon(entry)
            holder.icon.setImageDrawable(entry.icon)
            val packageState = thermalUtils.getStateForPackage(entry.info.packageName)
            holder.mode.setSelection(packageState, false)
            holder.mode.tag = entry
            holder.stateIcon.setImageResource(getStateDrawable(packageState))
        }

        fun setEntries(
            entries: List<ApplicationsState.AppEntry>,
            sections: List<String>,
            positions: List<Int>
        ) {
            this.entries = entries
            this.sections = sections.toTypedArray()
            this.positions = positions.toIntArray()
            notifyDataSetChanged()
        }

        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            val entry = parent.tag as? ApplicationsState.AppEntry ?: return
            val currentState = thermalUtils.getStateForPackage(entry.info.packageName)
            if (currentState != position) {
                thermalUtils.writePackage(entry.info.packageName, position)
                notifyDataSetChanged()
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}

        override fun getPositionForSection(section: Int): Int {
            return if (section < 0 || section >= sections.size) -1 else positions[section]
        }

        override fun getSectionForPosition(position: Int): Int {
            if (position < 0 || position >= itemCount) return -1
            val index = Arrays.binarySearch(positions, position)
            return if (index >= 0) index else -index - 2
        }

        override fun getSections(): Array<Any> = sections as Array<Any>
    }

    private inner class ActivityFilter(private val packageManager: PackageManager) :
        ApplicationsState.AppFilter {

        private val launcherResolveInfoList = mutableListOf<String>()

        init {
            updateLauncherInfoList()
        }

        fun updateLauncherInfoList() {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfoList = packageManager.queryIntentActivities(intent, 0)

            synchronized(launcherResolveInfoList) {
                launcherResolveInfoList.clear()
                resolveInfoList.forEach {
                    launcherResolveInfoList.add(it.activityInfo.packageName)
                }
            }
        }

        override fun init() {}

        override fun filterApp(entry: ApplicationsState.AppEntry): Boolean {
            var show = !allPackagesAdapter.entries.contains(entry.info.packageName)
            if (show) {
                synchronized(launcherResolveInfoList) {
                    show = launcherResolveInfoList.contains(entry.info.packageName)
                }
            }
            return show
        }
    }
}

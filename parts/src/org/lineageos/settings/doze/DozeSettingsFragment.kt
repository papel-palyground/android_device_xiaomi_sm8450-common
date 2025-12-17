/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

package org.lineageos.settings.doze

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.CompoundButton
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import androidx.preference.SwitchPreferenceCompat

import com.android.settingslib.widget.MainSwitchPreference

import org.lineageos.settings.R
import org.lineageos.settings.utils.FileUtils

class DozeSettingsFragment : PreferenceFragment(),
    Preference.OnPreferenceChangeListener,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var switchBar: MainSwitchPreference
    private lateinit var alwaysOnDisplayPreference: SwitchPreferenceCompat
    private lateinit var dozeBrightnessPreference: ListPreference

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.doze_settings)

        val prefs = activity.getSharedPreferences("doze_settings", Activity.MODE_PRIVATE)
        if (savedInstanceState == null && !prefs.getBoolean("first_help_shown", false)) {
            showHelp()
        }

        val dozeEnabled = DozeUtils.isDozeEnabled(activity)

        switchBar = findPreference(DozeUtils.DOZE_ENABLE) as MainSwitchPreference
        switchBar.addOnSwitchChangeListener(this)
        switchBar.isChecked = dozeEnabled

        alwaysOnDisplayPreference = findPreference(DozeUtils.ALWAYS_ON_DISPLAY) as SwitchPreferenceCompat
        alwaysOnDisplayPreference.isEnabled = dozeEnabled
        alwaysOnDisplayPreference.isChecked = DozeUtils.isAlwaysOnEnabled(activity)
        alwaysOnDisplayPreference.onPreferenceChangeListener = this

        dozeBrightnessPreference = findPreference(DozeUtils.DOZE_BRIGHTNESS_KEY) as ListPreference
        dozeBrightnessPreference.isEnabled = dozeEnabled && DozeUtils.isAlwaysOnEnabled(activity)
        dozeBrightnessPreference.onPreferenceChangeListener = this

        // Hide AOD and doze brightness if not supported
        if (!DozeUtils.alwaysOnDisplayAvailable(activity)) {
            preferenceScreen.removePreference(alwaysOnDisplayPreference)
            preferenceScreen.removePreference(dozeBrightnessPreference)
        } else {
            if (!FileUtils.isFileWritable(DozeUtils.DOZE_MODE_PATH)) {
                preferenceScreen.removePreference(dozeBrightnessPreference)
            } else {
                DozeUtils.updateDozeBrightnessIcon(context, dozeBrightnessPreference)
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference.key) {
            DozeUtils.ALWAYS_ON_DISPLAY -> {
                val enabled = newValue as Boolean
                DozeUtils.enableAlwaysOn(activity, enabled)
                if (!enabled) {
                    dozeBrightnessPreference.value = DozeUtils.DOZE_BRIGHTNESS_LBM
                    DozeUtils.setDozeMode(DozeUtils.DOZE_BRIGHTNESS_LBM)
                }
                dozeBrightnessPreference.isEnabled = enabled
            }
            DozeUtils.DOZE_BRIGHTNESS_KEY -> {
                val value = newValue as String
                if (value != DozeUtils.DOZE_BRIGHTNESS_AUTO) {
                    DozeUtils.setDozeMode(value)
                }
            }
        }

        handler.post {
            DozeUtils.checkDozeService(activity)
            DozeUtils.updateDozeBrightnessIcon(context, dozeBrightnessPreference)
        }

        return true
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        DozeUtils.enableDoze(activity, isChecked)
        DozeUtils.checkDozeService(activity)

        switchBar.isChecked = isChecked

        if (!isChecked) {
            DozeUtils.enableAlwaysOn(activity, false)
            alwaysOnDisplayPreference.isChecked = false
            dozeBrightnessPreference.value = DozeUtils.DOZE_BRIGHTNESS_LBM
            DozeUtils.updateDozeBrightnessIcon(context, dozeBrightnessPreference)
        }
        alwaysOnDisplayPreference.isEnabled = isChecked
        dozeBrightnessPreference.isEnabled = isChecked && DozeUtils.isAlwaysOnEnabled(activity)
    }

    private fun showHelp() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.doze_settings_help_title)
            .setMessage(R.string.doze_settings_help_text)
            .setPositiveButton(R.string.dialog_ok) { dialog, _ ->
                activity.getSharedPreferences("doze_settings", Activity.MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_help_shown", true)
                    .commit()
                dialog.cancel()
            }
            .create()
            .show()
    }
}

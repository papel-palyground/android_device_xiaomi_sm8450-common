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

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.display.AmbientDisplayConfiguration
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager

import org.lineageos.settings.R
import org.lineageos.settings.utils.FileUtils

object DozeUtils {
    private const val TAG = "DozeUtils"
    private const val DEBUG = false

    const val DOZE_ENABLE = "doze_enable"
    const val ALWAYS_ON_DISPLAY = "always_on_display"
    const val DOZE_BRIGHTNESS_KEY = "doze_brightness"

    const val DOZE_MODE_PATH = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/doze_mode"
    const val DOZE_MODE_HBM = "1"
    const val DOZE_MODE_LBM = "0"

    const val DOZE_BRIGHTNESS_LBM = "0"
    const val DOZE_BRIGHTNESS_HBM = "1"
    const val DOZE_BRIGHTNESS_AUTO = "2"

    @JvmStatic
    fun onBootCompleted(context: Context) {
        checkDozeService(context)
        restoreDozeModes(context)
    }

    @JvmStatic
    fun startService(context: Context) {
        if (DEBUG) Log.d(TAG, "Starting service")
        context.startServiceAsUser(Intent(context, DozeService::class.java), UserHandle.CURRENT)
    }

    @JvmStatic
    fun stopService(context: Context) {
        if (DEBUG) Log.d(TAG, "Stopping service")
        context.stopServiceAsUser(Intent(context, DozeService::class.java), UserHandle.CURRENT)
    }

    @JvmStatic
    fun checkDozeService(context: Context) {
        if (isDozeEnabled(context) && (isAlwaysOnEnabled(context) || sensorsEnabled(context))) {
            startService(context)
        } else {
            stopService(context)
        }
    }

    private fun restoreDozeModes(context: Context) {
        if (isAlwaysOnEnabled(context) && !isDozeAutoBrightnessEnabled(context)) {
            setDozeMode(
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(DOZE_BRIGHTNESS_KEY, DOZE_BRIGHTNESS_LBM) ?: DOZE_BRIGHTNESS_LBM
            )
        }
    }

    @JvmStatic
    fun enableDoze(context: Context, enable: Boolean): Boolean {
        return Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.DOZE_ENABLED,
            if (enable) 1 else 0
        )
    }

    @JvmStatic
    fun isDozeEnabled(context: Context): Boolean {
        return Settings.Secure.getInt(context.contentResolver, Settings.Secure.DOZE_ENABLED, 1) != 0
    }

    @JvmStatic
    fun enableAlwaysOn(context: Context, enable: Boolean): Boolean {
        return Settings.Secure.putIntForUser(
            context.contentResolver,
            Settings.Secure.DOZE_ALWAYS_ON,
            if (enable) 1 else 0,
            UserHandle.USER_CURRENT
        )
    }

    @JvmStatic
    fun isAlwaysOnEnabled(context: Context): Boolean {
        val enabledByDefault = context.resources.getBoolean(
            com.android.internal.R.bool.config_dozeAlwaysOnEnabled
        )
        return Settings.Secure.getIntForUser(
            context.contentResolver,
            Settings.Secure.DOZE_ALWAYS_ON,
            if (alwaysOnDisplayAvailable(context) && enabledByDefault) 1 else 0,
            UserHandle.USER_CURRENT
        ) != 0
    }

    @JvmStatic
    fun alwaysOnDisplayAvailable(context: Context): Boolean {
        return AmbientDisplayConfiguration(context).alwaysOnAvailable()
    }

    @JvmStatic
    fun setDozeMode(value: String): Boolean {
        return FileUtils.writeLine(DOZE_MODE_PATH, value)
    }

    @JvmStatic
    fun isDozeAutoBrightnessEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(DOZE_BRIGHTNESS_KEY, DOZE_BRIGHTNESS_LBM) == DOZE_BRIGHTNESS_AUTO
    }

    @JvmStatic
    fun sensorsEnabled(context: Context): Boolean {
        return isDozeAutoBrightnessEnabled(context)
    }

    @JvmStatic
    fun getSensor(sm: SensorManager, type: String): Sensor? {
        return sm.getSensorList(Sensor.TYPE_ALL).find { it.stringType == type }
    }

    @JvmStatic
    fun updateDozeBrightnessIcon(context: Context, preference: ListPreference) {
        val icon = when (PreferenceManager.getDefaultSharedPreferences(context)
            .getString(DOZE_BRIGHTNESS_KEY, DOZE_BRIGHTNESS_LBM)) {
            DOZE_BRIGHTNESS_LBM -> R.drawable.ic_doze_brightness_low
            DOZE_BRIGHTNESS_HBM -> R.drawable.ic_doze_brightness_high
            DOZE_BRIGHTNESS_AUTO -> R.drawable.ic_doze_brightness_auto
            else -> R.drawable.ic_doze_brightness_low
        }
        preference.setIcon(icon)
    }
}

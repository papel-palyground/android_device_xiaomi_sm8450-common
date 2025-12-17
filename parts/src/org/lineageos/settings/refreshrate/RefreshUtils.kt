/*
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

package org.lineageos.settings.refreshrate

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.UserHandle
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.preference.PreferenceManager

class RefreshUtils(private val context: Context) {

    companion object {
        private const val REFRESH_CONTROL = "refresh_control"
        private const val KEY_PEAK_REFRESH_RATE = "peak_refresh_rate"
        private const val KEY_MIN_REFRESH_RATE = "min_refresh_rate"

        private var defaultMaxRate = 120f
        private var defaultMinRate = 120f

        @JvmField
        var isAppInList = false

        const val STATE_DEFAULT = 0
        const val STATE_60 = 1
        const val STATE_90 = 2
        const val STATE_120 = 3
        const val STATE_60_LAND = 4
        const val STATE_90_LAND = 5
        const val STATE_120_LAND = 6

        private const val REFRESH_STATE_DEFAULT = 120f
        private const val REFRESH_STATE_60 = 60f
        private const val REFRESH_STATE_90 = 90f
        private const val REFRESH_STATE_120 = 120f
        private const val REFRESH_STATE_60_LAND = 60f
        private const val REFRESH_STATE_90_LAND = 90f
        private const val REFRESH_STATE_120_LAND = 120f

        private const val REFRESH_60 = "refresh.60="
        private const val REFRESH_90 = "refresh.90="
        private const val REFRESH_120 = "refresh.120="
        private const val REFRESH_60_LAND = "refresh.60land="
        private const val REFRESH_90_LAND = "refresh.90land="
        private const val REFRESH_120_LAND = "refresh.120land="

        @JvmStatic
        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, RefreshService::class.java),
                UserHandle.CURRENT
            )
        }
    }

    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    private var orientationListener: OrientationEventListener? = null
    private var isLandscape = false

    private fun writeValue(profiles: String) {
        sharedPrefs.edit().putString(REFRESH_CONTROL, profiles).apply()
    }

    fun getOldRate() {
        defaultMaxRate = Settings.System.getFloat(
            context.contentResolver,
            KEY_PEAK_REFRESH_RATE,
            REFRESH_STATE_DEFAULT
        )
        defaultMinRate = Settings.System.getFloat(
            context.contentResolver,
            KEY_MIN_REFRESH_RATE,
            REFRESH_STATE_DEFAULT
        )
    }

    private fun initializeOrientationListener(packageName: String) {
        orientationListener?.disable()

        orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                val currentOrientation = context.resources.configuration.orientation
                val newIsLandscape = currentOrientation == Configuration.ORIENTATION_LANDSCAPE
                if (newIsLandscape != isLandscape) {
                    isLandscape = newIsLandscape
                    adjustRefreshRateForOrientation(packageName)
                }
            }
        }

        orientationListener?.let {
            if (it.canDetectOrientation()) it.enable() else it.disable()
        }
    }

    private fun adjustRefreshRateForOrientation(packageName: String) {
        val state = getStateForPackage(packageName)
        val currentOrientation = context.resources.configuration.orientation
        val isLandscape = currentOrientation == Configuration.ORIENTATION_LANDSCAPE

        val (minRate, maxRate) = when (state) {
            STATE_60_LAND -> if (isLandscape) REFRESH_STATE_60_LAND to REFRESH_STATE_60_LAND
                             else defaultMinRate to defaultMaxRate
            STATE_90_LAND -> if (isLandscape) REFRESH_STATE_90_LAND to REFRESH_STATE_90_LAND
                             else defaultMinRate to defaultMaxRate
            STATE_120_LAND -> if (isLandscape) REFRESH_STATE_120_LAND to REFRESH_STATE_120_LAND
                              else defaultMinRate to defaultMaxRate
            else -> return
        }

        Settings.System.putFloat(context.contentResolver, KEY_MIN_REFRESH_RATE, minRate)
        Settings.System.putFloat(context.contentResolver, KEY_PEAK_REFRESH_RATE, maxRate)
    }

    fun checkOrientationAndSetRate(packageName: String) {
        val currentOrientation = context.resources.configuration.orientation
        val isCurrentlyLandscape = currentOrientation == Configuration.ORIENTATION_LANDSCAPE

        when {
            isCurrentlyLandscape && isAppInList -> setLandscapeModeRefreshRate(packageName)
            !isCurrentlyLandscape && isAppInList -> setPortraitModeRefreshRate(packageName)
        }
    }

    private fun disableOrientationListener() {
        orientationListener?.disable()
        orientationListener = null
    }

    fun setRefreshRate(packageName: String) {
        val value = getValue()
        val modes = value.split(":")
        var maxRate = defaultMaxRate
        var minRate = defaultMinRate
        isAppInList = false

        when {
            modes[0].contains("$packageName,") -> { // 60Hz
                disableOrientationListener()
                maxRate = REFRESH_STATE_60
                minRate = REFRESH_STATE_60
                isAppInList = true
            }
            modes[1].contains("$packageName,") -> { // 90Hz
                disableOrientationListener()
                maxRate = REFRESH_STATE_90
                minRate = REFRESH_STATE_90
                isAppInList = true
            }
            modes[2].contains("$packageName,") -> { // 120Hz
                disableOrientationListener()
                maxRate = REFRESH_STATE_120
                minRate = REFRESH_STATE_120
                isAppInList = true
            }
            modes[3].contains("$packageName,") -> { // 60Hz in landscape
                initializeOrientationListener(packageName)
                isAppInList = true
                return
            }
            modes[4].contains("$packageName,") -> { // 90Hz in landscape
                initializeOrientationListener(packageName)
                isAppInList = true
                return
            }
            modes[5].contains("$packageName,") -> { // 120Hz in landscape
                initializeOrientationListener(packageName)
                isAppInList = true
                return
            }
            else -> { // default
                disableOrientationListener()
                maxRate = defaultMaxRate
                minRate = defaultMinRate
            }
        }

        Settings.System.putFloat(context.contentResolver, KEY_MIN_REFRESH_RATE, minRate)
        Settings.System.putFloat(context.contentResolver, KEY_PEAK_REFRESH_RATE, maxRate)
    }

    private fun setLandscapeModeRefreshRate(packageName: String) {
        val (minRate, maxRate) = when (getStateForPackage(packageName)) {
            STATE_60_LAND -> REFRESH_STATE_60_LAND to REFRESH_STATE_60_LAND
            STATE_90_LAND -> REFRESH_STATE_90_LAND to REFRESH_STATE_90_LAND
            STATE_120_LAND -> REFRESH_STATE_120_LAND to REFRESH_STATE_120_LAND
            else -> return
        }
        Settings.System.putFloat(context.contentResolver, KEY_PEAK_REFRESH_RATE, maxRate)
        Settings.System.putFloat(context.contentResolver, KEY_MIN_REFRESH_RATE, minRate)
    }

    private fun setPortraitModeRefreshRate(packageName: String) {
        val state = getStateForPackage(packageName)
        if (state == STATE_60_LAND || state == STATE_90_LAND || state == STATE_120_LAND) {
            Settings.System.putFloat(context.contentResolver, KEY_PEAK_REFRESH_RATE, defaultMaxRate)
            Settings.System.putFloat(context.contentResolver, KEY_MIN_REFRESH_RATE, defaultMinRate)
        }
    }

    private fun getValue(): String {
        var value = sharedPrefs.getString(REFRESH_CONTROL, null)

        if (value.isNullOrEmpty()) {
            value = "$REFRESH_60:$REFRESH_90:$REFRESH_120:$REFRESH_60_LAND:$REFRESH_90_LAND:$REFRESH_120_LAND"
            writeValue(value)
        }

        val modes = value.split(":").toMutableList()
        if (modes.size < 6) {
            val defaults = listOf(REFRESH_60, REFRESH_90, REFRESH_120, REFRESH_60_LAND, REFRESH_90_LAND, REFRESH_120_LAND)
            while (modes.size < 6) {
                modes.add(defaults[modes.size])
            }
            value = modes.joinToString(":")
            writeValue(value)
        }

        return value
    }

    fun writePackage(packageName: String, mode: Int) {
        var value = getValue()
        value = value.replace("$packageName,", "")
        val modes = value.split(":").toMutableList()

        when (mode) {
            STATE_60 -> modes[0] = modes[0] + "$packageName,"
            STATE_90 -> modes[1] = modes[1] + "$packageName,"
            STATE_120 -> modes[2] = modes[2] + "$packageName,"
            STATE_60_LAND -> modes[3] = modes[3] + "$packageName,"
            STATE_90_LAND -> modes[4] = modes[4] + "$packageName,"
            STATE_120_LAND -> modes[5] = modes[5] + "$packageName,"
        }

        writeValue(modes.joinToString(":"))
    }

    fun getStateForPackage(packageName: String): Int {
        val value = getValue()
        val modes = value.split(":")

        return when {
            modes[0].contains("$packageName,") -> STATE_60
            modes[1].contains("$packageName,") -> STATE_90
            modes[2].contains("$packageName,") -> STATE_120
            modes[3].contains("$packageName,") -> STATE_60_LAND
            modes[4].contains("$packageName,") -> STATE_90_LAND
            modes[5].contains("$packageName,") -> STATE_120_LAND
            else -> STATE_DEFAULT
        }
    }
}

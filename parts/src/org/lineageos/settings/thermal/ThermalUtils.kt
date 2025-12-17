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

package org.lineageos.settings.thermal

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import androidx.preference.PreferenceManager

import org.lineageos.settings.utils.FileUtils

class ThermalUtils(context: Context) {

    companion object {
        private const val THERMAL_CONTROL = "thermal_control"
        private const val THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig"

        const val STATE_DEFAULT = 0
        const val STATE_BENCHMARK = 1
        const val STATE_BROWSER = 2
        const val STATE_CAMERA = 3
        const val STATE_DIALER = 4
        const val STATE_GAMING = 5
        const val STATE_NAVIGATION = 6
        const val STATE_STREAMING = 7
        const val STATE_VIDEO = 8

        private const val THERMAL_STATE_DEFAULT = "0"
        private const val THERMAL_STATE_BENCHMARK = "10"
        private const val THERMAL_STATE_BROWSER = "11"
        private const val THERMAL_STATE_CAMERA = "12"
        private const val THERMAL_STATE_DIALER = "8"
        private const val THERMAL_STATE_GAMING = "9"
        private const val THERMAL_STATE_NAVIGATION = "19"
        private const val THERMAL_STATE_STREAMING = "14"
        private const val THERMAL_STATE_VIDEO = "21"

        private const val THERMAL_BENCHMARK = "thermal.benchmark="
        private const val THERMAL_BROWSER = "thermal.browser="
        private const val THERMAL_CAMERA = "thermal.camera="
        private const val THERMAL_DIALER = "thermal.dialer="
        private const val THERMAL_GAMING = "thermal.gaming="
        private const val THERMAL_NAVIGATION = "thermal.navigation="
        private const val THERMAL_STREAMING = "thermal.streaming="
        private const val THERMAL_VIDEO = "thermal.video="

        @JvmStatic
        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, ThermalService::class.java),
                UserHandle.CURRENT
            )
        }
    }

    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    private fun writeValue(profiles: String) {
        sharedPrefs.edit().putString(THERMAL_CONTROL, profiles).apply()
    }

    private fun getValue(): String {
        var value = sharedPrefs.getString(THERMAL_CONTROL, null)

        if (value.isNullOrEmpty()) {
            value = "$THERMAL_BENCHMARK:$THERMAL_BROWSER:$THERMAL_CAMERA:$THERMAL_DIALER:" +
                    "$THERMAL_GAMING:$THERMAL_NAVIGATION:$THERMAL_STREAMING:$THERMAL_VIDEO"
            writeValue(value)
        }
        return value
    }

    fun writePackage(packageName: String, mode: Int) {
        var value = getValue()
        value = value.replace("$packageName,", "")
        val modes = value.split(":").toMutableList()

        when (mode) {
            STATE_BENCHMARK -> modes[0] = modes[0] + "$packageName,"
            STATE_BROWSER -> modes[1] = modes[1] + "$packageName,"
            STATE_CAMERA -> modes[2] = modes[2] + "$packageName,"
            STATE_DIALER -> modes[3] = modes[3] + "$packageName,"
            STATE_GAMING -> modes[4] = modes[4] + "$packageName,"
            STATE_NAVIGATION -> modes[5] = modes[5] + "$packageName,"
            STATE_STREAMING -> modes[6] = modes[6] + "$packageName,"
            STATE_VIDEO -> modes[7] = modes[7] + "$packageName,"
        }

        writeValue(modes.joinToString(":"))
    }

    fun getStateForPackage(packageName: String): Int {
        val value = getValue()
        val modes = value.split(":")

        return when {
            modes[0].contains("$packageName,") -> STATE_BENCHMARK
            modes[1].contains("$packageName,") -> STATE_BROWSER
            modes[2].contains("$packageName,") -> STATE_CAMERA
            modes[3].contains("$packageName,") -> STATE_DIALER
            modes[4].contains("$packageName,") -> STATE_GAMING
            modes[5].contains("$packageName,") -> STATE_NAVIGATION
            modes[6].contains("$packageName,") -> STATE_STREAMING
            modes[7].contains("$packageName,") -> STATE_VIDEO
            else -> STATE_DEFAULT
        }
    }

    fun setDefaultThermalProfile() {
        FileUtils.writeLine(THERMAL_SCONFIG, THERMAL_STATE_DEFAULT)
    }

    fun setThermalProfile(packageName: String) {
        val value = getValue()
        val modes = value.split(":")

        val state = when {
            modes[0].contains("$packageName,") -> THERMAL_STATE_BENCHMARK
            modes[1].contains("$packageName,") -> THERMAL_STATE_BROWSER
            modes[2].contains("$packageName,") -> THERMAL_STATE_CAMERA
            modes[3].contains("$packageName,") -> THERMAL_STATE_DIALER
            modes[4].contains("$packageName,") -> THERMAL_STATE_GAMING
            modes[5].contains("$packageName,") -> THERMAL_STATE_NAVIGATION
            modes[6].contains("$packageName,") -> THERMAL_STATE_STREAMING
            modes[7].contains("$packageName,") -> THERMAL_STATE_VIDEO
            else -> THERMAL_STATE_DEFAULT
        }

        FileUtils.writeLine(THERMAL_SCONFIG, state)
    }
}

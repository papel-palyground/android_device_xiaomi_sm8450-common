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

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

import org.lineageos.settings.R
import org.lineageos.settings.utils.FileUtils

class ThermalTileService : TileService() {

    companion object {
        private const val TAG = "ThermalTileService"
        private const val THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig"
    }

    private lateinit var modes: Array<String>
    private var currentMode = 0

    override fun onStartListening() {
        super.onStartListening()
        modes = arrayOf(
            getString(R.string.thermal_mode_default),
            getString(R.string.thermal_mode_performance),
            getString(R.string.thermal_mode_battery_saver),
            getString(R.string.thermal_mode_gaming)
        )
        currentMode = getCurrentThermalMode()

        if (currentMode == -1) {
            currentMode = 0
            setThermalMode(0)
        }

        updateTile()
    }

    override fun onClick() {
        toggleThermalMode()
    }

    private fun toggleThermalMode() {
        currentMode = (currentMode + 1) % modes.size
        setThermalMode(currentMode)
        updateTile()
    }

    private fun getCurrentThermalMode(): Int {
        val line = FileUtils.readOneLine(THERMAL_SCONFIG)
        if (line != null) {
            try {
                return when (line.trim().toInt()) {
                    20 -> 0 // Default
                    10 -> 1 // Performance
                    3 -> 2  // Battery Saver
                    9 -> 3  // Gaming
                    else -> 0
                }
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Error parsing thermal mode value: ", e)
            }
        }
        return -1
    }

    private fun setThermalMode(mode: Int) {
        val thermalValue = when (mode) {
            0 -> 20 // Default
            1 -> 10 // Performance
            2 -> 3  // Battery Saver
            3 -> 9  // Gaming
            else -> 20
        }

        val success = FileUtils.writeLine(THERMAL_SCONFIG, thermalValue.toString())
        Log.d(TAG, "Thermal mode changed to ${modes[mode]}: $success")
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            tile.label = "Thermal Profile"
            tile.subtitle = modes[currentMode]
            tile.updateTile()
        }
    }
}

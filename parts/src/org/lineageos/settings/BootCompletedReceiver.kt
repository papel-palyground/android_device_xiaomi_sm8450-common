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

package org.lineageos.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import org.lineageos.settings.doze.DozeUtils
import org.lineageos.settings.thermal.ThermalUtils
import org.lineageos.settings.refreshrate.RefreshUtils

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val DEBUG = false
        private const val TAG = "XiaomiParts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) Log.i(TAG, "Received intent: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> handleLockedBootCompleted(context)
            Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
        }
    }

    private fun handleLockedBootCompleted(context: Context) {
        if (DEBUG) Log.i(TAG, "Handling locked boot completed.")
        try {
            startServices(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error during locked boot completed processing", e)
        }
    }

    private fun handleBootCompleted(context: Context) {
        if (DEBUG) Log.i(TAG, "Handling boot completed.")
        // Add additional boot-completed actions if needed
    }

    private fun startServices(context: Context) {
        if (DEBUG) Log.i(TAG, "Starting services...")

        // Initialize Doze features
        DozeUtils.onBootCompleted(context)

        // Start Thermal Management Services
        ThermalUtils.startService(context)
        RefreshUtils.startService(context)
    }
}

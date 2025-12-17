/*
 * Copyright (C) 2024 The LineageOS Project
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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import androidx.preference.PreferenceManager

class Startup : BroadcastReceiver() {

    companion object {
        private const val TAG = "Startup"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive called with action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_REBOOT) {
            // Adding a delay before applying the saturation
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Applying saved saturation setting...")
                applySavedSaturation(context)
            }, 5000) // Delay of 5 seconds
        }
    }

    private fun applySavedSaturation(context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val seekBarValue = sharedPrefs.getInt(Constants.KEY_SATURATION, 100)
        Log.d(TAG, "Retrieved seekBarValue: $seekBarValue")

        // Apply the saved saturation value
        applySaturation(seekBarValue)
    }

    private fun applySaturation(seekBarValue: Int) {
        Log.d(TAG, "Applying saturation: $seekBarValue")

        val saturation = if (seekBarValue == 100) {
            1.001f
        } else {
            seekBarValue / 100.0f
        }

        val surfaceFlinger: IBinder? = ServiceManager.getService("SurfaceFlinger")
        if (surfaceFlinger != null) {
            try {
                val data = Parcel.obtain()
                data.writeInterfaceToken("android.ui.ISurfaceComposer")
                data.writeFloat(saturation)
                surfaceFlinger.transact(1022, data, null, 0)
                data.recycle()
                Log.d(TAG, "Saturation applied successfully")
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to apply saturation", e)
            }
        } else {
            Log.e(TAG, "SurfaceFlinger service not found")
        }
    }
}

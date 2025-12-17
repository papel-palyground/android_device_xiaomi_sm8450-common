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

import android.app.ActivityTaskManager
import android.app.Service
import android.app.TaskStackListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.RemoteException
import android.util.Log

class ThermalService : Service() {

    companion object {
        private const val TAG = "ThermalService"
        private const val DEBUG = false
    }

    private var screenOn = true
    private var currentApp = ""
    private lateinit var thermalUtils: ThermalUtils

    private val intentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    setThermalProfile()
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    setThermalProfile()
                }
            }
        }
    }

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")
        try {
            val activityTaskManager = ActivityTaskManager.getService()
            activityTaskManager.registerTaskStackListener(taskListener)
        } catch (e: RemoteException) {
            // Do nothing
        }
        thermalUtils = ThermalUtils(this)
        registerReceiver()
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(intentReceiver, filter)
    }

    private fun setThermalProfile() {
        if (screenOn) {
            thermalUtils.setThermalProfile(currentApp)
        } else {
            thermalUtils.setDefaultThermalProfile()
        }
    }

    private val taskListener = object : TaskStackListener() {
        override fun onTaskStackChanged() {
            try {
                val focusedTask = ActivityTaskManager.getService().focusedRootTaskInfo
                if (focusedTask?.topActivity != null) {
                    val foregroundApp = focusedTask.topActivity.packageName
                    if (foregroundApp != currentApp) {
                        currentApp = foregroundApp
                        setThermalProfile()
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
